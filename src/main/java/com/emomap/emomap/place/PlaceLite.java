package com.emomap.emomap.place;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PlaceLite {
    private String id;
    private String name;
    private String roadAddress;
    private double lat;
    private double lng;
    private String kakaoUrl;
    private String phone;
    private String categoryGroupCode;
    private String categoryName;
}
