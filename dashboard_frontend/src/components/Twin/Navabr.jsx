import React from 'react';
import ProductionTarget from '../KPI/ProductionTarget';
import HourlyProduction from '../KPI/HourlyProduction';
import CycleTime from '../KPI/CycleTime';
import ProductStatus from './ProductStatus';

const Navbar = ({ selectedProduct }) => {  
  return (
    <div>
      {/* 상단 날씨 카드들 */}
      <div className="row g-3 mb-4">
        {/* 날씨 카드 */}
        <div className="col-4">
          <div className="card h-100" role="button" onClick={() => console.log('날씨 클릭')} style={{cursor: 'pointer'}}>
            <div className="card-body text-center">
              <div className="mb-2">
                <svg xmlns="http://www.w3.org/2000/svg" width="32" height="32" viewBox="0 0 24 24" fill="#FF6F00" className="icon icon-tabler icons-tabler-filled icon-tabler-sun">
                  <path stroke="none" d="M0 0h24v24H0z" fill="none"/>
                  <path d="M12 19a1 1 0 0 1 .993 .883l.007 .117v1a1 1 0 0 1 -1.993 .117l-.007 -.117v-1a1 1 0 0 1 1 -1z" />
                  <path d="M18.313 16.91l.094 .083l.7 .7a1 1 0 0 1 -1.32 1.497l-.094 -.083l-.7 -.7a1 1 0 0 1 1.218 -1.567l.102 .07z" />
                  <path d="M7.007 16.993a1 1 0 0 1 .083 1.32l-.083 .094l-.7 .7a1 1 0 0 1 -1.497 -1.32l.083 -.094l.7 -.7a1 1 0 0 1 1.414 0z" />
                  <path d="M4 11a1 1 0 0 1 .117 1.993l-.117 .007h-1a1 1 0 0 1 -.117 -1.993l.117 -.007h1z" />
                  <path d="M21 11a1 1 0 0 1 .117 1.993l-.117 .007h-1a1 1 0 0 1 -.117 -1.993l.117 -.007h1z" />
                  <path d="M6.213 4.81l.094 .083l.7 .7a1 1 0 0 1 -1.32 1.497l-.094 -.083l-.7 -.7a1 1 0 0 1 1.217 -1.567l.102 .07z" />
                  <path d="M19.107 4.893a1 1 0 0 1 .083 1.32l-.083 .094l-.7 .7a1 1 0 0 1 -1.497 -1.32l.083 -.094l.7 -.7a1 1 0 0 1 1.414 0z" />
                  <path d="M12 2a1 1 0 0 1 .993 .883l.007 .117v1a1 1 0 0 1 -1.993 .117l-.007 -.117v-1a1 1 0 0 1 1 -1z" />
                  <path d="M12 7a5 5 0 1 1 -4.995 5.217l-.005 -.217l.005 -.217a5 5 0 0 1 4.995 -4.783z" />
                </svg>
              </div>
              <h6 className="card-title mb-1">날씨</h6>
              <p className="card-text text-muted">맑음</p>
            </div>
          </div>
        </div>
        
        {/* 온도 카드 */}
        <div className="col-4">
          <div className="card h-100" role="button" onClick={() => console.log('온도 클릭')} style={{cursor: 'pointer'}}>
            <div className="card-body text-center d-flex flex-column justify-content-center">
              <div className="mb-2">
                <svg xmlns="http://www.w3.org/2000/svg" width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="url(#tempGradientNavbar)" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <defs>
                    <linearGradient id="tempGradientNavbar" x1="0%" y1="0%" x2="0%" y2="100%">
                      <stop offset="0%" stopColor="#FF6B6B"/>
                      <stop offset="50%" stopColor="#FFA94D"/>
                      <stop offset="100%" stopColor="#4DABF7"/>
                    </linearGradient>
                  </defs>
                  <path stroke="none" d="M0 0h24v24H0z" fill="none"/>
                  <path d="M10 13.5a4 4 0 1 0 4 0v-8.5a2 2 0 0 0 -4 0v8.5" />
                  <path d="M10 9l4 0" />
                </svg>
              </div>
              <h6 className="card-title mb-1">온도</h6>
              <p className="card-text text-muted">25°C</p>
            </div>
          </div>
        </div>
        
        {/* 습도 카드 */}
        <div className="col-4">
          <div className="card h-100" role="button" onClick={() => console.log('습도 클릭')} style={{cursor: 'pointer'}}>
            <div className="card-body text-center d-flex flex-column justify-content-center">
              <div className="mb-2">
                <svg xmlns="http://www.w3.org/2000/svg" width="32" height="32" viewBox="0 0 24 24" fill="#3fa9f5" className="icon icon-tabler icons-tabler-filled icon-tabler-droplet">
                  <path stroke="none" d="M0 0h24v24H0z" fill="none" />
                  <path d="M10.708 2.372a2.382 2.382 0 0 0 -.71 .686l-4.892 7.26c-1.981 3.314 -1.22 7.466 1.767 9.882c2.969 2.402 7.286 2.402 10.254 0c2.987 -2.416 3.748 -6.569 1.795 -9.836l-4.919 -7.306c-.722 -1.075 -2.192 -1.376 -3.295 -.686z" />
                </svg>
              </div>
              <h6 className="card-title mb-1">습도</h6>
              <p className="card-text text-muted">55%</p>
            </div>
          </div>
        </div>
      </div>
      
      {/* 하단 KPI 카드들 */}
      <div className="row g-3">
        <div className="col-sm-12">
          <ProductionTarget />
        </div>
        <div className="col-6">
          <HourlyProduction />
        </div>
        <div className="col-6">
          <CycleTime />
        </div>
        <div className="col-sm-12">
          <ProductStatus selectedProduct={selectedProduct} />
        </div>
      </div>
    </div>
  );
}

export default Navbar;