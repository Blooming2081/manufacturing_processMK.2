import React from 'react';

const ProductStatus = ({ selectedProduct }) => {
  if (!selectedProduct) {
    return (
      <div className="card">
        <div className="card-body">
          <h3 className="card-title">제품 상태</h3>
          <div className="text-center py-3">
            <div className="text-muted">
              <svg xmlns="http://www.w3.org/2000/svg" width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1" strokeLinecap="round" strokeLinejoin="round">
                <path d="M7 17m-2 0a2 2 0 1 0 4 0a2 2 0 1 0 -4 0"></path>
                <path d="M17 17m-2 0a2 2 0 1 0 4 0a2 2 0 1 0 -4 0"></path>
                <path d="M5 17h-2v-6l2 -5h9l4 5h1a2 2 0 0 1 2 2v4h-2m-4 0h-6m-6 -6h15m-6 0v-5"></path>
              </svg>
            </div>
            <div className="text-muted small mt-2">
              제품을 선택하여<br/>상세 정보를 확인하세요
            </div>
          </div>
        </div>
      </div>
    );
  }

  const getStatusInfo = (status) => {
    switch (status) {
      case 'NORMAL':
        return { color: 'success', icon: '✅', text: '정상' };
      case 'WARNING':
        return { color: 'warning', icon: '⚠️', text: '주의' };
      case 'ERROR':
        return { color: 'danger', icon: '❌', text: '오류' };
      default:
        return { color: 'secondary', icon: '❓', text: '알 수 없음' };
    }
  };

  const statusInfo = getStatusInfo(selectedProduct.status);
  const progressPercentage = (selectedProduct.progress * 100).toFixed(1);

  return (
    <div className="card">
      <div className="card-body">
        <h3 className="card-title">제품 상태</h3>
        
        {/* 제품 기본 정보 */}
        <div className="row mb-3">
          <div className="col-6">
            <div className="text-muted small">제품 코드</div>
            <div className="fw-bold">{selectedProduct.id}</div>
          </div>
          <div className="col-6">
            <div className="text-muted small">차종</div>
            <div className="fw-bold">{selectedProduct.carType}</div>
          </div>
        </div>

        {/* 현재 상태 */}
        <div className="mb-3">
          <div className="d-flex align-items-center">
            <span className="me-2" style={{ fontSize: '1.2em' }}>{statusInfo.icon}</span>
            <span className={`badge bg-${statusInfo.color} me-2`}>{statusInfo.text}</span>
            <span className="text-muted small">현재 상태</span>
          </div>
        </div>

        {/* 현재 위치 */}
        <div className="mb-3">
          <div className="text-muted small">현재 위치</div>
          <div className="fw-bold">{selectedProduct.currentStation}</div>
        </div>

        {/* 진행률 */}
        <div className="mb-3">
          <div className="d-flex justify-content-between align-items-center mb-1">
            <span className="text-muted small">진행률</span>
            <span className="fw-bold">{progressPercentage}%</span>
          </div>
          <div className="progress" style={{ height: '8px' }}>
            <div 
              className={`progress-bar bg-${statusInfo.color}`}
              style={{ width: `${progressPercentage}%` }}
            ></div>
          </div>
        </div>

        {/* 시간 정보 */}
        <div className="row text-center mt-3">
          <div className="col-6">
            <div className="text-muted small">시작 시간</div>
            <div className="fw-bold small">{selectedProduct.startTime}</div>
          </div>
          <div className="col-6">
            <div className="text-muted small">예상 완료</div>
            <div className="fw-bold small">{selectedProduct.estimatedCompletion}</div>
          </div>
        </div>

        {/* 추가 정보 */}
        {selectedProduct.issues && selectedProduct.issues.length > 0 && (
          <div className="mt-3">
            <div className="text-muted small mb-1">발생 이슈</div>
            <div className="alert alert-warning py-2">
              <small>{selectedProduct.issues.join(', ')}</small>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default ProductStatus;