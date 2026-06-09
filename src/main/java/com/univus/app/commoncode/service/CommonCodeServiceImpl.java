package com.univus.app.commoncode.service;

import com.univus.app.commoncode.dto.CommonCodeResponseDto;
import com.univus.app.commoncode.mapper.CommonCodeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CommonCodeServiceImpl implements CommonCodeService {

    private final CommonCodeMapper commonCodeMapper;

    @Override
    @Transactional(readOnly = true)
    public List<CommonCodeResponseDto> getCodesByGroupCode(String groupCode) {
        return commonCodeMapper.selectCodesByGroupCode(groupCode);
    }
}
