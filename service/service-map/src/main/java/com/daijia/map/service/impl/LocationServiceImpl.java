package com.daijia.map.service.impl;

import com.daijia.common.constant.RedisConstant;
import com.daijia.common.constant.SystemConstant;
import com.daijia.common.result.Result;
import com.daijia.common.util.LocationUtil;
import com.daijia.driver.client.DriverInfoFeignClient;
import com.daijia.map.repository.OrderServiceLocationRepository;
import com.daijia.map.service.LocationService;
import com.daijia.model.entity.driver.DriverSet;
import com.daijia.model.entity.map.OrderServiceLocation;
import com.daijia.model.form.map.OrderServiceLocationForm;
import com.daijia.model.form.map.SearchNearByDriverForm;
import com.daijia.model.form.map.UpdateDriverLocationForm;
import com.daijia.model.form.map.UpdateOrderLocationForm;
import com.daijia.model.vo.map.NearByDriverVo;
import com.daijia.model.vo.map.OrderLocationVo;
import com.daijia.model.vo.map.OrderServiceLastLocationVo;
import com.daijia.order.client.OrderInfoFeignClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Sort;
import org.springframework.data.geo.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.domain.geo.GeoLocation;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/*

RedisTemplate	用途	序列化方式	适用场景
redisTemplate	存储对象	JSON	用户信息、订单信息等需要存对象的场景
stringRedisTemplate	存储字符串	String	GEO操作、计数器、简单键值对
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LocationServiceImpl implements LocationService {

    // 只修改这里：从 RedisTemplate 改为 StringRedisTemplate 存储司机位置信息
    private final StringRedisTemplate stringRedisTemplate;
    private final DriverInfoFeignClient driverInfoFeignClient;
    private final RedisTemplate redisTemplate;
    private final OrderServiceLocationRepository orderServiceLocationRepository;
    private final MongoTemplate mongoTemplate;
    private final OrderInfoFeignClient orderInfoFeignClient;

    //更新司机位置信息
    @Override
    public Boolean updateDriverLocation(UpdateDriverLocationForm updateDriverLocationForm) {
        //把司机位置信息添加redis里面geo
        Point point = new Point(updateDriverLocationForm.getLongitude().doubleValue(),
                updateDriverLocationForm.getLatitude().doubleValue());
        //添加到redis里面
        stringRedisTemplate.opsForGeo().add(RedisConstant.DRIVER_GEO_LOCATION,
                point,
                updateDriverLocationForm.getDriverId().toString());
        return true;
    }

    //删除司机位置信息
    @Override
    public Boolean removeDriverLocation(Long driverId) {
        stringRedisTemplate.opsForGeo().remove(RedisConstant.DRIVER_GEO_LOCATION, driverId.toString());
        return true;
    }

    //搜索附近满足条件的司机(优化前)
    //@Override
    public List<NearByDriverVo> searchNearByDriver1(SearchNearByDriverForm searchNearByDriverForm) {
        //搜索经纬度位置5公里以内的司机
        //1 操作redis里面geo
        //创建point，经纬度位置
        Point point = new Point(searchNearByDriverForm.getLongitude().doubleValue(),
                searchNearByDriverForm.getLatitude().doubleValue());

        //定义距离，5公里
        Distance distance = new Distance(SystemConstant.NEARBY_DRIVER_RADIUS,
                RedisGeoCommands.DistanceUnit.KILOMETERS);

        //创建circle对象，point  distance
        Circle circle = new Circle(point, distance);

        //定义GEO参数，设置返回结果包含内容
        RedisGeoCommands.GeoRadiusCommandArgs args =
                RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs()
                        .includeDistance()  //包含距离
                        .includeCoordinates() //包含坐标
                        .sortAscending(); //升序

        GeoResults<RedisGeoCommands.GeoLocation<String>> result =
                stringRedisTemplate.opsForGeo().radius(RedisConstant.DRIVER_GEO_LOCATION, circle, args);

        //2 查询redis最终返回list集合
        List<GeoResult<RedisGeoCommands.GeoLocation<String>>> content = result.getContent();

        //3 对查询list集合进行处理
        // 遍历list集合，得到每个司机信息
        // 根据每个司机个性化设置信息判断
        List<NearByDriverVo> list = new ArrayList<>();
        if(!CollectionUtils.isEmpty(content)) {
            Iterator<GeoResult<RedisGeoCommands.GeoLocation<String>>> iterator = content.iterator();
            while(iterator.hasNext()) {
                GeoResult<RedisGeoCommands.GeoLocation<String>> item = iterator.next();

                //获取司机id
                Long driverId = Long.parseLong(item.getContent().getName());

                //远程调用，根据司机id个性化设置信息
                Result<DriverSet> driverSetResult = driverInfoFeignClient.getDriverSet(driverId);
                DriverSet driverSet = driverSetResult.getData();

                //判断订单里程order_distance
                BigDecimal orderDistance = driverSet.getOrderDistance();
                log.info("订单里程: ", distance);
                //orderDistance==0，司机没有限制的
                //如果不等于0 ，比如30，接单30公里代驾订单。
                //接单距离 - 当前单子距离  < 0,不符合条件
                // 30          35
                if(orderDistance.doubleValue() != 0
                        && orderDistance.subtract(searchNearByDriverForm.getMileageDistance()).doubleValue()<0) {
                    continue;
                }

                //判断接单里程 accept_distance
                //当前接单距离
                BigDecimal currentDistance =
                        new BigDecimal(item.getDistance().getValue()).setScale(2, RoundingMode.HALF_UP);

                BigDecimal acceptDistance = driverSet.getAcceptDistance();
                if(acceptDistance.doubleValue() !=0
                        && acceptDistance.subtract(currentDistance).doubleValue()<0) {
                    continue;
                }

                //封装复合条件数据
                NearByDriverVo nearByDriverVo = new NearByDriverVo();
                nearByDriverVo.setDriverId(driverId);
                nearByDriverVo.setDistance(currentDistance);
                list.add(nearByDriverVo);

            }

        }
        return list;
    }

    /**
     * 优化版：搜索附近司机（无循环远程调用，批量缓存+本地过滤）
     */
    @Override
    public List<NearByDriverVo> searchNearByDriver(SearchNearByDriverForm searchNearByDriverForm) {
        // 1. 封装经纬度
        Point point = new Point(
                searchNearByDriverForm.getLongitude().doubleValue(),
                searchNearByDriverForm.getLatitude().doubleValue()
        );

        // 2. 5公里范围
        Distance distance = new Distance(
                SystemConstant.NEARBY_DRIVER_RADIUS,
                RedisGeoCommands.DistanceUnit.KILOMETERS
        );
        Circle circle = new Circle(point, distance);

        // 3. GEO 查询参数
        RedisGeoCommands.GeoRadiusCommandArgs args =
                RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs()
                        .includeDistance()
                        .includeCoordinates()
                        .sortAscending();

        // 4. Redis GEO 核心查询（超快）
        GeoResults<RedisGeoCommands.GeoLocation<String>> results =
                stringRedisTemplate.opsForGeo()
                        .radius(RedisConstant.DRIVER_GEO_LOCATION, circle, args);

        List<GeoResult<RedisGeoCommands.GeoLocation<String>>> content = results.getContent();
        if (CollectionUtils.isEmpty(content)) {
            return Collections.emptyList();
        }

        // ======================= 优化点开始 =======================
        // 5. 批量提取司机ID（关键：一次性提取所有ID）
        List<Long> driverIds = content.stream()
                .map(item -> Long.parseLong(item.getContent().getName()))
                .collect(Collectors.toList());

        // 6. 批量从数据库获取司机配置（1次批量操作，替代N次Feign）
        Map<Long, DriverSet> driverSetMap = driverInfoFeignClient.batchGetDriverSet(driverIds).getData();
        // ======================= 优化点结束 =======================

        List<NearByDriverVo> resultList = new ArrayList<>();

        // 7. 本地内存遍历过滤（无任何IO，极快）
        for (GeoResult<RedisGeoCommands.GeoLocation<String>> item : content) {
            Long driverId = Long.parseLong(item.getContent().getName());
            DriverSet driverSet = driverSetMap.get(driverId);
            if (driverSet == null) {
                continue;
            }

            // 过滤条件1：订单里程限制
            BigDecimal orderDistance = driverSet.getOrderDistance();
            if (orderDistance.doubleValue() != 0
                    && orderDistance.compareTo(searchNearByDriverForm.getMileageDistance()) < 0) {
                continue;
            }

            // 过滤条件2：接单距离限制
            BigDecimal currentDistance = new BigDecimal(item.getDistance().getValue())
                    .setScale(2, RoundingMode.HALF_UP);
            BigDecimal acceptDistance = driverSet.getAcceptDistance();
            if (acceptDistance.doubleValue() != 0
                    && acceptDistance.compareTo(currentDistance) < 0) {
                continue;
            }

            // 封装符合条件的司机
            NearByDriverVo vo = new NearByDriverVo();
            vo.setDriverId(driverId);
            vo.setDistance(currentDistance);
            resultList.add(vo);
        }

        return resultList;
    }

    //司机赶往代驾起始点：更新订单地址到缓存
    @Override
    public Boolean updateOrderLocationToCache(UpdateOrderLocationForm updateOrderLocationForm) {

        OrderLocationVo orderLocationVo = new OrderLocationVo();
        orderLocationVo.setLongitude(updateOrderLocationForm.getLongitude());
        orderLocationVo.setLatitude(updateOrderLocationForm.getLatitude());

        String key = RedisConstant.UPDATE_ORDER_LOCATION + updateOrderLocationForm.getOrderId();
        redisTemplate.opsForValue().set(key,orderLocationVo);
        return true;
    }

    // 乘客端查询司机位置信息(司乘同显)
    @Override
    public OrderLocationVo getCacheOrderLocation(Long orderId) {
        String key = RedisConstant.UPDATE_ORDER_LOCATION + orderId;
        OrderLocationVo orderLocationVo = (OrderLocationVo)redisTemplate.opsForValue().get(key);
        return orderLocationVo;
    }

    // 存储司机的GPS定位信息,定时批量上传到后台服务器
    @Override
    public Boolean saveOrderServiceLocation(List<OrderServiceLocationForm> orderLocationServiceFormList) {

        List<OrderServiceLocation> list = new ArrayList<>();
        //OrderServiceLocation
        orderLocationServiceFormList.forEach(orderServiceLocationForm->{
            //orderServiceLocationForm -- OrderServiceLocation
            OrderServiceLocation orderServiceLocation = new OrderServiceLocation();
            BeanUtils.copyProperties(orderServiceLocationForm,orderServiceLocation);
            orderServiceLocation.setId(ObjectId.get().toString());
            orderServiceLocation.setCreateTime(new Date());

            list.add(orderServiceLocation);
            //orderServiceLocationRepository.save(orderServiceLocation);
        });
        // MySQL单表记录超过千万行就开始变慢了,所以批量添加到MongoDB中
        orderServiceLocationRepository.saveAll(list);
        return true;
    }

    // 乘客端获取司机动向,定时获取上面更新的最后一个位置信息
    @Override
    public OrderServiceLastLocationVo getOrderServiceLastLocation(Long orderId) {
        //查询MongoDB
        //查询条件 ：orderId
        //根据创建时间降序排列
        //最新一条数据
        Query query = new Query();
        query.addCriteria(Criteria.where("orderId").is(orderId));
        query.with(Sort.by(Sort.Order.desc("createTime")));
        query.limit(1);

        OrderServiceLocation orderServiceLocation =
                mongoTemplate.findOne(query, OrderServiceLocation.class);
        OrderServiceLastLocationVo orderServiceLastLocationVo = new OrderServiceLastLocationVo();
        BeanUtils.copyProperties(orderServiceLocation,orderServiceLastLocationVo);
        return orderServiceLastLocationVo;
    }

    @Override
    public BigDecimal calculateOrderRealDistance(Long orderId) {
        //1 根据订单id获取代驾订单位置信息，根据创建时间排序（升序）
        //查询MongoDB
        //第一种方式
        //        OrderServiceLocation orderServiceLocation = new OrderServiceLocation();
        //        orderServiceLocation.setOrderId(orderId);
        //        Example<OrderServiceLocation> example = Example.of(orderServiceLocation);
        //        Sort sort = Sort.by(Sort.Direction.ASC, "createTime");
        //        List<OrderServiceLocation> list = orderServiceLocationRepository.findAll(example, sort);
        //第二种方式
        //MongoRepository只需要 按照规则 在MongoRepository把查询方法创建出来就可以了
        // 总体规则：
        //1 查询方法名称 以 get  |  find  | read开头
        //2 后面查询字段名称，满足驼峰式命名，比如OrderId
        //3 字段查询条件添加关键字，比如Like  OrderBy   Asc
        // 具体编写 ： 根据订单id获取代驾订单位置信息，根据创建时间排序（升序）
        List<OrderServiceLocation> list =
                orderServiceLocationRepository.findByOrderIdOrderByCreateTimeAsc(orderId);

        //2 第一步查询返回订单位置信息list集合
        //把list集合遍历，得到每个位置信息，计算两个位置距离
        //把计算所有距离相加操作
        double realDistance = 0;
        if(!CollectionUtils.isEmpty(list)) {
            for (int i = 0,size = list.size()-1; i < size; i++) {
                OrderServiceLocation location1 = list.get(i);
                OrderServiceLocation location2 = list.get(i + 1);

                //计算位置距离
                double distance = LocationUtil.getDistance(location1.getLatitude().doubleValue(),
                        location1.getLongitude().doubleValue(),
                        location2.getLatitude().doubleValue(),
                        location2.getLongitude().doubleValue());

                realDistance += distance;
            }
        }

        //TODO 为了测试，不好测试实际代驾距离，模拟数据  实际距离=预估距离+5公里
        if(realDistance == 0) {
            return orderInfoFeignClient.getOrderInfo(orderId).getData().getExpectDistance().add(new BigDecimal("5"));
        }

        //3 返回最终计算实际距离
        return new BigDecimal(realDistance);
    }


}