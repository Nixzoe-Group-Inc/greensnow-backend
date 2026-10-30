package com.ayitech.worker.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserListResponse {

    private String responseCode;
    private String responseMessage;
    private Integer totalUsers;
    private Integer page;
    private Integer size;
    private Integer totalPages;
    private List<UserDetail> users;
}
