package com.hotelvista.property;

import java.util.List;

public record PropertyPage(List<PropertyResponse> items, int page, int size, boolean hasNext) { }
