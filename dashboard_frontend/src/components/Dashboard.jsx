import React, { useState, useEffect } from 'react';
import ProductionStatus from './KPI/ProductionStatus';
import OTDStatus from './KPI/OTDStatus';
import FTYStatus from './KPI/FTYStatus';
import ProductionTarget from './KPI/ProductionTarget';
import HourlyProduction from './KPI/HourlyProduction';
import CycleTime from './KPI/CycleTime';
import RobotTables from './Robot/RobotTables';
import InventoryStatus from './Inventory/InventoryTable';
import AlertToast from './Alert/AlertToast';
import apiService from '../service/apiService';
import useWebSocket from '../hooks/useWebSocket';

const Dashboard = () => {
  const [dashboardData, setDashboardData] = useState(null);
  const [stationsData] = useState([]);
  const [loading, setLoading] = useState(true);
  const [lastUpdated, setLastUpdated] = useState(new Date());
  const [connectionStatus, setConnectionStatus] = useState('connecting');

  // 사용자 정보 가져오기 (localStorage에서)
  const [userInfo, setUserInfo] = useState(null);
  const [companyInfo, setCompanyInfo] = useState(null);

  // 현재 로그인된 사용자 정보 로드
  useEffect(() => {
    const loadUserInfo = () => {
      try {
        // localStorage에서 실제 로그인된 사용자 정보 가져오기
        const storedUserData = localStorage.getItem('userData');
        const isLoggedIn = localStorage.getItem('isLoggedIn');
        
        if (isLoggedIn === 'true' && storedUserData) {
          const userData = JSON.parse(storedUserData);
          console.log('저장된 사용자 데이터:', userData);
          
          setCompanyInfo({
            companyName: userData.companyName,
            companyId: userData.companyId
          });
          setUserInfo({
            userId: userData.userId,
            userName: userData.userName
          });
        } else {
          // 로그인되지 않은 경우 기본값 사용
          const defaultCompany = { companyName: '유원대학교' };
          const defaultUser = { userId: 'testuser', userName: '테스트사용자' };
          
          setCompanyInfo(defaultCompany);
          setUserInfo(defaultUser);
        }
      } catch (error) {
        console.error('사용자 정보 로드 실패:', error);
        // 에러 시 기본값 사용
        const defaultCompany = { companyName: '유원대학교' };
        const defaultUser = { userId: 'testuser', userName: '테스트사용자' };
        
        setCompanyInfo(defaultCompany);
        setUserInfo(defaultUser);
      }
    };

    loadUserInfo();
  }, []);

  // WebSocket 연결 및 알림 관리
  const { alerts, removeAlert, realtimeData } = useWebSocket(
    companyInfo?.companyName || '유원대학교', 
    userInfo?.userId || 'anonymous'
  );

  // 실시간 데이터 처리
  useEffect(() => {
    if (realtimeData) {
      console.log('실시간 데이터 수신:', realtimeData);
      
      // 데이터 타입에 따라 처리
      if (realtimeData.type === 'environment_update') {
        console.log('환경 데이터 업데이트:', realtimeData.data);
        setLastUpdated(new Date());
        // 환경 데이터 업데이트 로직 추가 가능
      } else if (realtimeData.type === 'kpi_update') {
        console.log('KPI 데이터 업데이트:', realtimeData.data);
        setLastUpdated(new Date());
        // KPI 데이터 업데이트 로직 추가 가능
      }
    }
  }, [realtimeData]);

  useEffect(() => {
    let mounted = true;
    let pollingInterval;

    // 데이터 가져오기 함수
    const fetchAllData = async () => {
      if (!mounted) return;

      try {
        setConnectionStatus('connecting');
        
        // 병렬로 모든 데이터 가져오기
        const [dashboardData, kpiData, environmentData, conveyorData] = await Promise.all([
          apiService.dashboard.getData(),
          apiService.dashboard.getRealTimeKPI(),
          apiService.environment.getCurrent(),
          apiService.production.getConveyorStatus()
        ]);

        if (mounted) {
          setDashboardData({
            ...dashboardData,
            kpi: kpiData,
            environment: environmentData,
            conveyor: conveyorData
          });
          setConnectionStatus('connected');
          setLastUpdated(new Date());
          setLoading(false);
        }
      } catch (error) {
        console.error('API 연결 실패:', error);
        if (mounted) {
          setConnectionStatus('error');
          setLoading(false);
        }
      }
    };

    // 초기 데이터 로드
    fetchAllData();

    // 3초마다 폴링
    pollingInterval = setInterval(fetchAllData, 3000);

    // 초기 로딩 타이머
    const loadingTimer = setTimeout(() => {
      if (mounted && loading) {
        setConnectionStatus('error');
        setLoading(false);
      }
    }, 10000);

    return () => {
      mounted = false;
      if (pollingInterval) {
        clearInterval(pollingInterval);
      }
      clearTimeout(loadingTimer);
    };
  }, [loading]);

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
                    current={dashboardData?.production?.today_completed || 0}
                    target={1000}
                  />
                </div>
                <div className="col-6">
                  <HourlyProduction 
                    rate={dashboardData?.production?.hourly_rate || 0}
                  />
                </div>
                <div className="col-6">
                  <CycleTime 
                    time={dashboardData?.production?.cycle_time || 0}
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
                <div className="text-muted small">
                  마지막 업데이트: {lastUpdated ? lastUpdated.toLocaleTimeString() : '연결 대기 중'}
                </div>
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
                    N/A
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* 알림 토스트 */}
      <AlertToast alerts={alerts} onRemove={removeAlert} />
    </div>
  );
};

export default Dashboard;