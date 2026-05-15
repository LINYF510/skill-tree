package com.fancy.skill_tree.core.domain.common

/**
 * 领域层异常基类
 * 封装所有业务逻辑相关的异常
 */
sealed class DomainException(
    override val message: String,
    override val cause: Throwable? = null
) : Exception(message, cause) {

    /**
     * 节点未找到异常
     * @param nodeId 未找到的节点 ID
     */
    class NodeNotFound(nodeId: String) : DomainException("节点未找到: $nodeId")

    /**
     * 父节点未找到异常
     * @param parentId 未找到的父节点 ID
     */
    class ParentNodeNotFound(parentId: String) : DomainException("父节点未找到: $parentId")

    /**
     * 循环引用异常
     * @param nodeId 要移动的节点 ID
     * @param parentId 目标父节点 ID
     */
    class CircularReference(nodeId: String, parentId: String) :
        DomainException("检测到循环引用: $nodeId -&gt; $parentId")

    /**
     * 子节点数超过上限异常
     * @param parentId 父节点 ID
     * @param max 最大允许的子节点数量
     */
    class MaxChildrenExceeded(parentId: String, max: Int) :
        DomainException("父节点 $parentId 子节点数超过上限 $max")

    /**
     * 无效的节点类型异常
     * @param type 无效的节点类型
     */
    class InvalidNodeType(type: String) : DomainException("无效的节点类型: $type")

    /**
     * 标签已存在异常
     * @param name 已存在的标签名称
     */
    class TagAlreadyExists(name: String) : DomainException("标签已存在: $name")

    /**
     * 链接已存在异常
     * @param sourceId 源节点 ID
     * @param targetId 目标节点 ID
     */
    class LinkAlreadyExists(sourceId: String, targetId: String) :
        DomainException("链接已存在: $sourceId &lt;-&gt; $targetId")

    /**
     * 自身链接不允许异常
     * @param nodeId 节点 ID
     */
    class SelfLinkNotAllowed(nodeId: String) : DomainException("不能链接自身: $nodeId")

    /**
     * 存储操作异常
     * @param cause 原始异常
     */
    class StorageError(cause: Throwable) : DomainException("存储操作失败", cause)

    /**
     * 验证失败异常
     * @param field 验证失败的字段名
     * @param reason 验证失败的原因
     */
    class ValidationError(field: String, reason: String) :
        DomainException("$field 验证失败: $reason")
}
