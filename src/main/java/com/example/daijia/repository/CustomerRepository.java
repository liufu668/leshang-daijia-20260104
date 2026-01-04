package com.example.daijia.repository;

import com.example.daijia.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository：负责数据访问，是底层数据库操作的抽象
 * Service：负责业务逻辑，协调多个 Repository 和组件完成业务功能
 * Controller：负责HTTP请求处理，调用 Service 完成业务
 * 这样分层的好处：
 * 单一职责：每层专注自己的事
 * 易于测试：可以单独测试 Service 逻辑，Mock Repository
 * 代码复用：多个 Service 可以共享同一个 Repository
 * 维护性好：数据库变更只需修改 Repository，不影响业务逻辑
 *
 *
 * 不用写的方法,Spring Data JPA 会自动提供标准的 CRUD 方法：
 * save(S entity)           - 保存/更新
 * findById(ID id)         - 根据ID查询
 * findAll()               - 查询所有
 * deleteById(ID id)       - 根据ID删除
 * count()                 - 统计数量
 * existsById(ID id)       - 判断是否存在
 * 等等...
 *
 *  要写的方法	示例	                 说明
 *  自定义查询	findByWxOpenId()	根据业务需求定制
 *  复杂查询	    @Query 注解方法	    JPQL或原生SQL
 *  统计方法	    countByStatus()	    聚合查询
 *  存在判断	    existsByXxx()	    检查唯一性
 *  删除方法	    deleteByXxx()	    条件删除
 */
@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Customer findByWxOpenId(String wxOpenId) ;

}
