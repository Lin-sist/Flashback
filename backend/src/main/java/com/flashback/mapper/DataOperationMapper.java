package com.flashback.mapper;

import com.flashback.domain.DataOperation;
import com.flashback.domain.DataOperationStatus;
import com.flashback.domain.DataOperationType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface DataOperationMapper {
    int insert(DataOperation operation);
    DataOperation selectByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);
    DataOperation selectLatestActiveByUser(@Param("userId") Long userId);
    int countActiveByUserAndTypes(@Param("userId") Long userId, @Param("types") List<DataOperationType> types);
    int countBlockingClearAllByUser(@Param("userId") Long userId);
    int updateIfStatus(
            @Param("operation") DataOperation operation,
            @Param("expectedStatus") DataOperationStatus expectedStatus);
    List<DataOperation> selectExpiredArtifacts(@Param("now") LocalDateTime now, @Param("limit") int limit);
    List<DataOperation> selectStaleRunnable(@Param("cutoff") LocalDateTime cutoff, @Param("limit") int limit);
}
