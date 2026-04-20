package com.nlt.common.api;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageData<T> {

    private List<T> list;

    private long total;

    private int pageNum;

    private int pageSize;

}

