package com.drukconnect.drukconnect.dto.authentication;

import java.util.List;

public record BuyerVouchesResponse(

        long totalVouchedUsers,

        List<BuyerVouchResponse> vouches

) {
}