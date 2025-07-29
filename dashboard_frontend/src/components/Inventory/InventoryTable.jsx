import React, { useState, useEffect } from 'react';
import apiService from '../../service/apiService';

const InventoryTable = () => {
  const [inventory, setInventory] = useState([]);
  const [currentPage, setCurrentPage] = useState(1);
  const [searchTerm, setSearchTerm] = useState('');
  const itemsPerPage = 5;

  // API 호출
  useEffect(() => {
    const fetchStockData = async () => {
      try {
        const response = await apiService.inventory.getStock();

        const mappedData = response.map(item => ({
          id: item.stockCode,
          name: item.stockName,
          location: item.stockLocation,
          currentStock: item.currentStock,
          safetyStock: item.safetyStock,
          consumptionRate: 'N/A',
          estimatedRunOut: 'N/A',
          status: item.stockState,
          lastSupply: item.inboundDate
            ? new Date(item.inboundDate).toLocaleString()
            : 'N/A',
        }));
        setInventory(mappedData);
      } catch (error) {
        console.error('Error fetching stock data:', error);
      }
    };

    fetchStockData();
  }, []);

  // 검색 필터 적용
  const filteredInventory = inventory.filter(item =>
    item.id.toLowerCase().includes(searchTerm.toLowerCase()) ||
    item.name.toLowerCase().includes(searchTerm.toLowerCase())
  );

  const totalPages = Math.ceil(filteredInventory.length / itemsPerPage);

  const paginatedInventory = filteredInventory.slice(
    (currentPage - 1) * itemsPerPage,
    currentPage * itemsPerPage
  );

  const handlePageChange = (page) => {
    if (page >= 1 && page <= totalPages) {
      setCurrentPage(page);
    }
  };

  const getStatusColor = (status) => {
    switch (status) {
      case '정상': return 'status-green';
      case '주의': return 'status-yellow';
      case '부족': return 'status-orange';
      case '긴급': return 'status-red';
      default: return 'status-gray';
    }
  };

  const getSupplyColor = (supply) => {
    if (supply.includes('완료')) return 'status-green';
    if (supply.includes('지연')) return 'status-red';
    return 'status-blue';
  };

  return (
    <div className="table-responsive">

      {/* 🔍 검색 입력창 */}
      <div className="mb-3 d-flex justify-content-end">
        <input
          type="text"
          className="form-control"
          placeholder="제품 코드 또는 제품명을 검색"
          value={searchTerm}
          onChange={(e) => {
            setSearchTerm(e.target.value);
            setCurrentPage(1); // 검색 시 1페이지로 리셋
          }}
        />
      </div>

      {/* 테이블 */}
      <table className="table table-vcenter">
        <thead>
          <tr>
            <th>제품 코드</th>
            <th className="text-nowrap">제품명</th>
            <th className="text-nowrap">위치</th>
            <th className="text-nowrap">현재재고</th>
            <th className="text-nowrap">안전재고</th>
            <th className="text-nowrap">소모율</th>
            <th className="text-nowrap">예상소진</th>
            <th className="text-nowrap">상태</th>
            <th className="text-nowrap">입고 시간</th>
          </tr>
        </thead>
        <tbody>
          {paginatedInventory.length > 0 ? (
            paginatedInventory.map((item) => (
              <tr key={item.id}>
                <th>{item.id}</th>
                <td>{item.name}</td>
                <td>{item.location}</td>
                <td>{item.currentStock}개</td>
                <td>{item.safetyStock}개</td>
                <td>{item.consumptionRate}</td>
                <td>{item.estimatedRunOut}</td>
                <td>
                  <span className={`status ${getStatusColor(item.status)}`}>
                    {item.status}
                  </span>
                </td>
                <td>
                  <span className={`status ${getSupplyColor(item.lastSupply)}`}>
                    {item.lastSupply}
                  </span>
                </td>
              </tr>
            ))
          ) : (
            <tr>
              <td colSpan="9" className="text-center py-5">
                <div className="text-muted">
                  <i className="ti ti-package fs-1 mb-3 d-block"></i>
                  <h5>등록되어 있는 재고가 없습니다</h5>
                  <p>현재 시스템에 등록된 재고가 없거나 데이터베이스 연결에 문제가 있습니다.</p>
                </div>
              </td>
            </tr>
          )}
        </tbody>
      </table>

      {/* Bootstrap 페이지네이션 */}
      <div className="d-flex justify-content-center mt-4">
        <ul className="pagination">
          <li className={`page-item ${currentPage === 1 ? 'disabled' : ''}`}>
            <button
              className="page-link page-text"
              onClick={() => handlePageChange(currentPage - 1)}
              disabled={currentPage === 1}
            >
              이전
            </button>
          </li>

          {Array.from({ length: totalPages }, (_, index) => (
            <li
              key={index}
              className={`page-item ${currentPage === index + 1 ? 'active' : ''}`}
            >
              <button
                className="page-link"
                onClick={() => handlePageChange(index + 1)}
              >
                {index + 1}
              </button>
            </li>
          ))}

          <li className={`page-item ${currentPage === totalPages ? 'disabled' : ''}`}>
            <button
              className="page-link page-text"
              onClick={() => handlePageChange(currentPage + 1)}
              disabled={currentPage === totalPages}
            >
              다음
            </button>
          </li>
        </ul>
      </div>

      {/* 범례 */}
      <div className="mt-3">
        <small className="text-muted">
          <strong>상태 기준:</strong>
          <span className="status status-green ms-1">정상</span> 안전재고 이상 |
          <span className="status status-yellow ms-1">주의</span> 안전재고 근접 |
          <span className="status status-orange ms-1">부족</span> 안전재고 미만 |
          <span className="status status-red ms-1">긴급</span> 2시간 내 소진
        </small>
      </div>
    </div>
  );
};

export default InventoryTable;
