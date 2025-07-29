package com.u1mobis.dashboard_backend.controller;

import com.u1mobis.dashboard_backend.dto.StockChartDTO;
import com.u1mobis.dashboard_backend.dto.StockSummaryDTO;
import com.u1mobis.dashboard_backend.entity.Stock;
import com.u1mobis.dashboard_backend.repository.StockRepository;
import com.u1mobis.dashboard_backend.service.StockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@RestController
@RequestMapping("/api/{companyName}")  // 멀티테넌트 경로
@Slf4j
public class StockController {

    private final StockRepository stockRepository;
    private final StockService stockService;

    public StockController(StockRepository stockRepository, StockService stockService) {
        this.stockRepository = stockRepository;
        this.stockService = stockService;
    }

    // 최근 10개 stock 리스트 반환
    @GetMapping("/stock")
    public List<Stock> getStockList(@PathVariable String companyName) {
        log.info("재고 리스트 요청 - 회사: {}", companyName);
        return stockRepository.findAll(PageRequest.of(0, 10)).getContent();
    }

    // 월별 차트 데이터 반환
    @GetMapping("/stocks/chart")
    public List<StockChartDTO> getChartData(@PathVariable String companyName) {
        log.info("재고 차트 데이터 요청 - 회사: {}", companyName);
        return stockService.getMonthlyChartData();
    }

    // 차량별 재고 요약 데이터 반환 (도넛 차트용, 차 이름 기반)
    @GetMapping("/stocks/summary")
    public List<StockSummaryDTO> getStockSummary(@PathVariable String companyName) {
        log.info("재고 요약 데이터 요청 - 회사: {}", companyName);
        return stockService.getStockSummary();
    }
}