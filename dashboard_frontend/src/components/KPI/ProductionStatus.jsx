import { useEffect, useRef } from 'react'
import ApexCharts from 'apexcharts'

const ProductionStatus = ({ oee = 61.2, oeeComponents = null }) => {
  const chartRef = useRef(null)

  useEffect(() => {
    const timer = setTimeout(() => {
      const chartElement = document.getElementById('chart-production-status')
      
      if (!chartElement) {
        console.error("Chart element not found")
        return
      }

      const oeeValue = parseFloat(oee);
      const remaining = 100 - oeeValue;

      // OEE 등급 계산
      const getOEEGrade = (value) => {
        if (value >= 85) return { grade: '우수', color: '#28a745' };
        if (value >= 70) return { grade: '양호', color: '#ffc107' };
        if (value >= 60) return { grade: '보통', color: '#fd7e14' };
        return { grade: '개선필요', color: '#dc3545' };
      };

      const gradeInfo = getOEEGrade(oeeValue);

      const options = {
        chart: {
          type: "donut",
          fontFamily: 'inherit',
          height: 200,
          animations: {
            enabled: true,
            easing: 'easeinout',
            speed: 800
          },
        },
        series: [oeeValue, remaining],
        labels: ["OEE", "미달성"],
        colors: [gradeInfo.color, '#e9ecef'],
        legend: {
          show: false
        },
        plotOptions: {
          pie: {
            donut: {
              size: '65%',
              labels: {
                show: true,
                total: {
                  show: true,
                  color: gradeInfo.color,
                  fontSize: '18px',
                  fontWeight: 600,
                  label: 'OEE',
                  formatter: function (w) {
                    return oeeValue.toFixed(1) + '%';
                  }
                }
              }
            }
          }
        },
        tooltip: {
          enabled: true,
          y: {
            formatter: function (val, opts) {
              if (opts.seriesIndex === 0) {
                // 실제 API에서 받은 OEE 구성요소 사용
                const availability = oeeComponents?.availability || 0;
                const performance = oeeComponents?.performance || 0;
                const quality = oeeComponents?.quality || 0;
                
                return `가동률: ${availability.toFixed(1)}%<br/>성능률: ${performance.toFixed(1)}%<br/>품질률: ${quality.toFixed(1)}%<br/>OEE: ${val.toFixed(1)}%<br/>등급: ${gradeInfo.grade}`;
              }
              return `미달성: ${val.toFixed(1)}%`;
            }
          }
        },
        dataLabels: {
          enabled: false
        },
        responsive: [{
          breakpoint: 480,
          options: {
            chart: {
              height: 180
            },
            legend: {
              fontSize: '10px'
            }
          }
        }]
      };

      try {
        if (chartRef.current) {
          chartRef.current.destroy();
        }
        
        chartRef.current = new ApexCharts(chartElement, options);
        chartRef.current.render();
      } catch (error) {
        console.error("Chart rendering error:", error);
      }
    }, 100);

    return () => {
      clearTimeout(timer);
      if (chartRef.current) {
        chartRef.current.destroy();
        chartRef.current = null;
      }
    };
  }, [oee, oeeComponents]); // oee 값과 구성요소가 변경될 때마다 차트 업데이트

  // OEE 등급 정보
  const getOEEGrade = (value) => {
    if (value >= 85) return { grade: '우수', color: 'text-success', bgColor: 'bg-success' };
    if (value >= 70) return { grade: '양호', color: 'text-warning', bgColor: 'bg-warning' };
    if (value >= 60) return { grade: '보통', color: 'text-info', bgColor: 'bg-info' };
    return { grade: '개선필요', color: 'text-danger', bgColor: 'bg-danger' };
  };

  const gradeInfo = getOEEGrade(parseFloat(oee));

  return (
    <div>
      <div id="chart-production-status" className="position-relative"></div>
      
      {/* OEE 상세 정보 */}
      <div className="mt-3">
        <div className="row text-center">
          <div className="col-6">
            <div className="text-muted small">등급</div>
            <div className={`fw-bold ${gradeInfo.color}`}>
              {gradeInfo.grade}
            </div>
          </div>
          <div className="col-6">
            <div className="text-muted small">목표</div>
            <div className="fw-bold text-primary">85%</div>
          </div>
        </div>
        
        {/* 진행률 바 */}
        <div className="mt-2">
          <div className="progress" style={{ height: '6px' }}>
            <div 
              className={`progress-bar ${gradeInfo.bgColor}`}
              style={{ width: `${Math.min(parseFloat(oee), 100)}%` }}
            ></div>
          </div>
          <div className="d-flex justify-content-between mt-1">
            <small className="text-muted">0%</small>
            <small className={gradeInfo.color}>{parseFloat(oee).toFixed(1)}%</small>
            <small className="text-muted">100%</small>
          </div>
        </div>
      </div>
    </div>
  );
};

export default ProductionStatus;