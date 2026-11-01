package com.cpoa.common;

import java.util.List;

/**
 * 分页响应体。
 */
public record PageResult<T>(List<T> records, long total, long current, long size) {
}
