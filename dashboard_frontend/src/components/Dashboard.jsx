import React, { useState, useEffect } from 'react';
import ProductionStatus from './KPI/ProductionStatus';
import OTDStatus from './KPI/OTDStatus';
import FTYStatus from './KPI/FTYStatus';
import ProductionTarget from './KPI/ProductionTarget';
import HourlyProduction from './KPI/HourlyProduction';
import CycleTime from './KPI/CycleTime';
import RobotTables from './Robot/RobotTables';
import InventoryStatus from './Inventory/InventoryTables';
import dashboardService from '../service/dashboardService';

const Dashboard = () => {
  const [dashboardData, setDashboardData] = useState(null);
  const [stationsData, setStationsData] = useState([]);
  const [loading, setLoading] = useState(true);
  const [lastUpdated, setLastUpdated] = useState(new Date());
  const [connectionStatus, setConnectionStatus] = useState('connecting');

  useEffect(() => {
    let mounted = true;

    // 데이터 업데이트 핸들러
    const handleDataUpdate = (type, data) => {
      if (!mounted) return;

      switch (type) {
        case 'dashboard':
          setDashboardData(data);
          setConnectionStatus('connected');
          break;
        case 'stations':
          setStationsData(data);
          break;
        case 'error':
          setConnectionStatus('error');
          break;
      }
      setLastUpdated(new Date());
      setLoading(false);
    };

    // 서비스 구독
    const unsubscribe = dashboardService.subscribe(handleDataUpdate);

    // 폴링 시작
    dashboardService.startPolling(3000); // 3초마다 업데이트

    // 초기 로딩 타이머
    const loadingTimer = setTimeout(() => {
      if (mounted && loading) {
        setConnectionStatus('error');
        setLoading(false);
      }
    }, 10000);

    return () => {
      mounted = false;
      unsubscribe();
      dashboardService.stopPolling();
      clearTimeout(loadingTimer);
    };
  }, []);

  // 연결 상태 표시
  const renderConnectionStatus = () => {
    const statusConfig = {
      connecting: { color: 'warning', text: '연결 중...', icon: '⏳' },
      connected: { color: 'success', text: '실시간 연결됨', icon: '🟢' },
      error: { color: 'danger', text: '연결 오류', icon: '🔴' }
    };

    const config = statusConfig[connectionStatus];
    
    return (
      <div className={`alert alert-${config.color} alert-dismissible fade show`} role="alert">
        {config.icon} {config.text} - 마지막 업데이트: {lastUpdated.toLocaleTimeString()}
        <small className="ms-2">
          (데이터 소스: Spring Boot API)
        </small>
      </div>
    );
  };

  if (loading) {
    return (
      <div className="container-xl">
        <div className="text-center my-5">
          <div className="spinner-border text-primary" role="status">
            <span className="visually-hidden">로딩 중...</span>
          </div>
          <div className="mt-3">대시보드 데이터를 불러오는 중...</div>
        </div>
      </div>
    );
  }

  return (
    <div>
      {/* 연결 상태 표시 */}
      {renderConnectionStatus()}
      
      <div className="row g-3">
        {/* 왼쪽 생산 목표 카드 */}
        <div className="col-sm-12 col-lg-4">
          <div className="card h-100">
            <div className="card-body">
              <div className="row g-3">
                <div className="col-sm-12">
                  <ProductionTarget 
                    current={dashboardData?.production?.current || 0}
                    target={dashboardData?.production?.target || 1000}
                  />
                </div>
                <div className="col-6">
                  <HourlyProduction 
                    rate={dashboardData?.production?.hourlyRate || 0}
                  />
                </div>
                <div className="col-6">
                  <CycleTime 
                    time={dashboardData?.production?.cycleTime || 0}
                  />
                </div>
              </div>
            </div>
          </div>
        </div>

        {/* 오른쪽 3*1 KPI 차트 카드 */}
        <div className="col-sm-12 col-lg-8">
          <div className="card h-100">
            <div className="card-body">
              <div className="row g-3">
                {/* OEE */}
                <div className="col-sm-12 col-md-4">
                  <div className="card">
                    <div className="card-body">
                      <h3 className="card-title">OEE(설비 종합 효율)</h3>
                      <ProductionStatus 
                        oee={dashboardData?.kpi?.oee || 0}
                      />
                    </div>
                  </div>
                </div>
                {/* OTD */}
                <div className="col-sm-12 col-md-4">
                  <div className="card">
                    <div className="card-body">
                      <h3 className="card-title">OTD(정기납기율)</h3>
                      <OTDStatus 
                        otd={dashboardData?.kpi?.otd || 0}
                      />
                    </div>
                  </div>
                </div>
                {/* FTY */}
                <div className="col-sm-12 col-md-4">
                  <div className="card">
                    <div className="card-body">
                      <h3 className="card-title">FTY(일발양품률)</h3>
                      <FTYStatus 
                        fty={dashboardData?.kpi?.fty || 0}
                      />
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        {/* 실시간 로봇 모니터링 */}
        <div className="col-12">
          <div className="card">
            <div className="card-body">
              <div className="d-flex justify-content-between align-items-center mb-3">
                <h3 className="card-title mb-0">실시간 스테이션 모니터링</h3>
                <span className="badge bg-primary">
                  {stationsData.length}개 스테이션 운영 중
                </span>
              </div>
              <RobotTables 
                stationsData={stationsData}
                lastUpdated={lastUpdated}
              />
            </div>
          </div>
        </div>

        {/* 재고 현황 */}
        <div className="col-12">
          <div className="card">
            <div className="card-body">
              <div className="d-flex justify-content-between align-items-center mb-3">
                <h3 className="card-title mb-0">재고 현황</h3>
                <small className="text-muted">
                  마지막 업데이트: {lastUpdated.toLocaleTimeString()}
                </small>
              </div>
              <InventoryStatus 
                dashboardData={dashboardData}
                stationsData={stationsData}
              />
            </div>
          </div>
        </div>

        {/* 시스템 정보 */}
        <div className="col-12">
          <div className="card">
            <div className="card-body">
              <h3 className="card-title">시스템 정보</h3>
              <div className="row">
                <div className="col-md-3">
                  <div className="text-muted small">데이터 소스</div>
                  <div className="h5">
                    {connectionStatus === 'connected' ? (
                      <span className="text-success">🟢 Spring Boot API</span>
                    ) : (
                      <span className="text-danger">🔴 연결 오류</span>
                    )}
                  </div>
                </div>
                <div className="col-md-3">
                  <div className="text-muted small">업데이트 주기</div>
                  <div className="h5">3초</div>
                </div>
                <div className="col-md-3">
                  <div className="text-muted small">마지막 업데이트</div>
                  <div className="h5">{lastUpdated.toLocaleString()}</div>
                </div>
                <div className="col-md-3">
                  <div className="text-muted small">전체 품질 점수</div>
                  <div className="h5 text-success">
                    {(parseFloat(dashboardData?.quality?.overallScore || 0) * 100).toFixed(1)}%
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Dashboard;