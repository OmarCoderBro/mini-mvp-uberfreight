package com.uberfreight.search.api;

import com.uberfreight.search.model.ShipmentSearchParams;
import com.uberfreight.search.model.ShipmentSearchResponse;
import com.uberfreight.search.search.ShipmentSearchException;
import com.uberfreight.search.search.ShipmentSearchService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class ShipmentController {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final ShipmentSearchService searchService;

    public ShipmentController(ShipmentSearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping("/shipments/search")
    public ShipmentSearchResponse search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String origin,
            @RequestParam(required = false) String destination,
            @RequestParam(name = "equipment_type", required = false) String equipmentType,
            @RequestParam(name = "min_rate", required = false) Double minRate,
            @RequestParam(name = "max_rate", required = false) Double maxRate,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "" + DEFAULT_PAGE_SIZE) int pageSize) {

        if (page < 1) {
            throw badRequest("page must be 1 or greater");
        }
        if (pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw badRequest("page_size must be between 1 and " + MAX_PAGE_SIZE);
        }
        if (minRate != null && minRate < 0) {
            throw badRequest("min_rate must be 0 or greater");
        }
        if (maxRate != null && maxRate < 0) {
            throw badRequest("max_rate must be 0 or greater");
        }
        if (minRate != null && maxRate != null && minRate > maxRate) {
            throw badRequest("min_rate must not be greater than max_rate");
        }

        ShipmentSearchParams params = new ShipmentSearchParams(
                q, origin, destination, equipmentType, minRate, maxRate, page, pageSize);
        return searchService.search(params);
    }

    @ExceptionHandler(ShipmentSearchException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public String handleSearchFailure(ShipmentSearchException e) {
        return "Search is temporarily unavailable";
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
