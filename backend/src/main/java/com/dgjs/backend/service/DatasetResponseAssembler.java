package com.dgjs.backend.service;

import com.dgjs.backend.domain.DatasetMeta;
import com.dgjs.backend.domain.Employee;
import com.dgjs.backend.dto.DatasetDto;
import com.dgjs.backend.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

// 목록·상세 응답에 소유자 이름, 프로젝트, 로그인한 사용자 기준 수정·삭제 가능 여부를 채움
@Component
@RequiredArgsConstructor
public class DatasetResponseAssembler {

    private final EmployeeRepository employeeRepository;
    private final DatasetAccessPolicy accessPolicy;

    public List<DatasetDto.SummaryResponse> summaries(
            List<DatasetMeta> metas,
            Map<Long, Long> rowCounts,
            Map<Long, Long> columnCounts,
            EmployeePrincipal user) {
        Map<String, String> ownerNames = ownerNames(metas);
        Set<String> myProjectCodes = accessPolicy.myProjectCodes(user);

        return metas.stream().map(meta -> DatasetDto.SummaryResponse.builder()
                .id(meta.getId())
                .name(meta.getName())
                .description(meta.getDescription())
                .category(meta.getCategory())
                .ownerId(meta.getOwnerId())
                .ownerName(nameOf(ownerNames, meta.getOwnerId()))
                .projectCode(DatasetAccessPolicy.projectCodeOf(meta))
                .projectName(meta.getProject() == null ? null : meta.getProject().getName())
                .canEdit(accessPolicy.canEdit(user, meta, myProjectCodes))
                .canDelete(accessPolicy.canManage(user, meta))
                .createdAt(meta.getCreatedAt())
                .lastLoadedAt(lastLoadedOf(meta))
                .rowCount(rowCounts.getOrDefault(meta.getId(), 0L))
                .columnCount(columnCounts.getOrDefault(meta.getId(), 0L).intValue())
                .build()).collect(Collectors.toList());
    }

    public DatasetDto.DetailResponse detail(
            DatasetMeta meta, List<DatasetDto.ColumnResponse> columns, EmployeePrincipal user) {
        return DatasetDto.DetailResponse.builder()
                .id(meta.getId())
                .name(meta.getName())
                .description(meta.getDescription())
                .category(meta.getCategory())
                .ownerId(meta.getOwnerId())
                .ownerName(nameOf(ownerNames(List.of(meta)), meta.getOwnerId()))
                .projectCode(DatasetAccessPolicy.projectCodeOf(meta))
                .projectName(meta.getProject() == null ? null : meta.getProject().getName())
                .canEdit(accessPolicy.canEdit(user, meta))
                .canDelete(accessPolicy.canManage(user, meta))
                .createdAt(meta.getCreatedAt())
                .lastLoadedAt(lastLoadedOf(meta))
                .columns(columns)
                .build();
    }

    // 소유자가 비어 있는 기존 데이터셋이 있을 수 있어 null 키 조회를 피함
    private String nameOf(Map<String, String> ownerNames, String ownerId) {
        return ownerId == null ? null : ownerNames.get(ownerId);
    }

    private Map<String, String> ownerNames(Collection<DatasetMeta> metas) {
        Set<String> ownerIds = metas.stream()
                .map(DatasetMeta::getOwnerId)
                .filter(id -> id != null && !id.isBlank())
                .collect(Collectors.toSet());
        if (ownerIds.isEmpty()) {
            return Map.of();
        }
        return employeeRepository.findAllById(ownerIds).stream()
                .collect(Collectors.toMap(Employee::getEmpNo, Employee::getName));
    }

    // 값이 없는 기존 데이터와 빈 수동 데이터셋은 등록일로 대체함
    private LocalDateTime lastLoadedOf(DatasetMeta meta) {
        return meta.getLastLoadedAt() != null ? meta.getLastLoadedAt() : meta.getCreatedAt();
    }
}
