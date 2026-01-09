package com.datacollect.dto;

import com.datacollect.entity.NetworkElement;
import com.datacollect.entity.NetworkElementAttribute;
import lombok.Data;

import java.util.List;

/**
 * 网元DTO，包含网元信息和属性列表
 *
 * @author system
 * @since 2025-01-20
 */
@Data
public class NetworkElementDTO {

    /**
     * 网元信息
     */
    private NetworkElement networkElement;

    /**
     * 网元属性列表
     */
    private List<NetworkElementAttribute> attributes;
}



