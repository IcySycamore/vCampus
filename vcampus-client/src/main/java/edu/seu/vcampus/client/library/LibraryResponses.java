package edu.seu.vcampus.client.library;

import edu.seu.vcampus.client.api.ApiErrors;
import edu.seu.vcampus.client.api.ApiException;
import edu.seu.vcampus.common.message.PageResponse;
import java.util.ArrayList;
import java.util.List;

/** 校验图书馆响应中的实体、列表与分页结构。 */
final class LibraryResponses {
    private LibraryResponses() {
    }

    static <T> T value(Object data, Class<T> type) {
        if (!type.isInstance(data)) {
            throw new ApiException(ApiErrors.LOCAL_MALFORMED);
        }
        return type.cast(data);
    }

    static <T> List<T> list(Object data, Class<T> type) {
        if (!(data instanceof List<?>)) {
            throw new ApiException(ApiErrors.LOCAL_MALFORMED);
        }
        List<T> result = new ArrayList<T>();
        for (Object item : (List<?>) data) {
            result.add(value(item, type));
        }
        return result;
    }

    static <T> PageResponse<T> page(Object data, Class<T> type) {
        if (!(data instanceof PageResponse<?>)) {
            throw new ApiException(ApiErrors.LOCAL_MALFORMED);
        }
        PageResponse<?> source = (PageResponse<?>) data;
        List<T> items = new ArrayList<T>();
        for (Object item : source.getItems()) {
            items.add(value(item, type));
        }
        if (items.size() > source.getPageSize() || source.getTotal() < items.size()) {
            throw new ApiException(ApiErrors.LOCAL_MALFORMED);
        }
        return new PageResponse<T>(items, source.getTotal(),
                source.getPageNumber(), source.getPageSize());
    }
}
