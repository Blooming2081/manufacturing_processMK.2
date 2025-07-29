import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import apiService from "../service/apiService";

const CompanyRegister = () => {
  const [companyName, setCompanyName] = useState("");
  const [companyCode, setCompanyCode] = useState("");
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [loading, setLoading] = useState(false);
  const [isRegistered, setIsRegistered] = useState(false);
  const navigate = useNavigate();

  // 회사명 중복 체크
  const checkCompanyName = async (name) => {
    if (!name.trim()) return;
    
    try {
      const exists = await apiService.company.checkCompanyName(name.trim());
      if (exists) {
        setError("이미 등록된 회사명입니다.");
        return false;
      } else {
        setError("");
        return true;
      }
    } catch (err) {
      console.error('회사명 중복 체크 오류:', err);
      return true; // 오류 시 진행 허용
    }
  };

  // 회사명 변경 시 중복 체크
  const handleCompanyNameChange = async (e) => {
    const name = e.target.value;
    setCompanyName(name);
    
    if (name.trim()) {
      await checkCompanyName(name);
    } else {
      setError("");
    }
  };


  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError("");
    setSuccess("");

    if (!companyName.trim()) {
      setError("회사명을 입력해주세요.");
      setLoading(false);
      return;
    }

    // 회사명 중복 체크
    const isNameValid = await checkCompanyName(companyName.trim());
    if (!isNameValid) {
      setLoading(false);
      return;
    }

    try {
      // 회사코드 없이 등록 (서버에서 자동 생성)
      const response = await apiService.company.register({
        companyName: companyName.trim()
      });

      if (response.success) {
        setSuccess("회사가 성공적으로 등록되었습니다!");
        setCompanyCode(response.companyCode); // 서버에서 생성된 코드 설정
        setIsRegistered(true); // 등록 완료 상태로 변경
      } else {
        setError(response.message || "회사 등록에 실패했습니다.");
      }
    } catch (err) {
      console.error('회사 등록 오류:', err);
      setError("서버 연결에 실패했습니다. 잠시 후 다시 시도해주세요.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="vh-100 d-flex justify-content-center align-items-center" 
         style={{ 
           background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
           position: 'relative',
           overflow: 'hidden'
         }}>
      
      {/* 배경 애니메이션 원들 */}
      <div style={{
        position: 'absolute',
        width: '300px',
        height: '300px',
        borderRadius: '50%',
        background: 'rgba(255, 255, 255, 0.1)',
        top: '-150px',
        right: '-150px',
        animation: 'float 6s ease-in-out infinite'
      }}></div>
      <div style={{
        position: 'absolute',
        width: '200px',
        height: '200px',
        borderRadius: '50%',
        background: 'rgba(255, 255, 255, 0.1)',
        bottom: '-100px',
        left: '-100px',
        animation: 'float 6s ease-in-out infinite 2s'
      }}></div>
      <div style={{
        position: 'absolute',
        width: '150px',
        height: '150px',
        borderRadius: '50%',
        background: 'rgba(255, 255, 255, 0.1)',
        top: '50%',
        right: '10%',
        animation: 'float 6s ease-in-out infinite 4s'
      }}></div>


      <div className="container">
        <div className="row justify-content-center">
          <div className="col-lg-6 col-xl-5">
            <div className="card border-0 shadow-lg" 
                 style={{ 
                   background: 'rgba(255, 255, 255, 0.95)',
                   backdropFilter: 'blur(20px)',
                   borderRadius: '24px'
                 }}>
              <div className="card-body p-5">
                <div className="text-center mb-4">
                  <div className="mb-3">
                    <i className="ti ti-building fs-1 text-primary"></i>
                  </div>
                  <h2 className="card-title fw-bold fs-2 mb-2">회사 등록</h2>
                  <p className="text-muted">새로운 회사를 등록하고 관리를 시작하세요.</p>
                </div>

                <form onSubmit={handleSubmit}>
                  <div className="mb-4">
                    <label className="form-label fw-semibold mb-2">
                      <i className="ti ti-building-store me-2"></i>
                      회사명
                    </label>
                    <input
                      type="text"
                      className="form-control form-control-lg border-2"
                      value={companyName}
                      onChange={handleCompanyNameChange}
                      placeholder="회사명을 입력하세요."
                      required
                      disabled={isRegistered}
                      style={{ 
                        borderColor: error && error.includes('회사명') ? '#dc3545' : '#e5e7eb', 
                        fontSize: '0.9rem',
                        backgroundColor: isRegistered ? '#f8f9fa' : 'white'
                      }}
                    />
                  </div>

                  {isRegistered && (
                    <div className="mb-4">
                      <label className="form-label fw-semibold mb-2">
                        <i className="ti ti-qrcode me-2"></i>
                        생성된 회사 코드
                      </label>
                      <div className="input-group">
                        <input
                          type="text"
                          className="form-control form-control-lg border-2"
                          value={companyCode}
                          style={{ 
                            borderColor: '#28a745', 
                            backgroundColor: '#f8fff9', 
                            fontSize: '1.1rem',
                            fontWeight: 'bold',
                            textAlign: 'center'
                          }}
                          readOnly
                        />
                        <button
                          type="button"
                          className="btn btn-success btn-lg"
                          onClick={() => {
                            navigator.clipboard.writeText(companyCode);
                            alert('회사코드가 클립보드에 복사되었습니다!');
                          }}
                        >
                          <i className="ti ti-copy me-1"></i>
                          복사
                        </button>
                      </div>
                      <div className="form-text text-success">
                        <i className="ti ti-check-circle me-1"></i>
                        이 코드를 직원들과 공유하세요. 직원 회원가입 시 필요합니다.
                      </div>
                    </div>
                  )}

                  {error && (
                    <div className="alert alert-danger d-flex align-items-center py-3 mb-4">
                      <i className="ti ti-alert-circle me-2"></i>
                      {error}
                    </div>
                  )}

                  {success && (
                    <div className="alert alert-success d-flex align-items-center py-3 mb-4">
                      <i className="ti ti-check-circle me-2"></i>
                      {success}
                    </div>
                  )}

                  {!isRegistered ? (
                    <button 
                      type="submit" 
                      className="btn btn-lg w-100 text-white fw-semibold mb-4"
                      style={{ 
                        background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
                        border: 'none'
                      }}
                      disabled={loading || !companyName.trim() || (error && error.includes('회사명'))}
                    >
                      {loading ? (
                        <>
                          <span className="spinner-border spinner-border-sm me-2" role="status"></span>
                          등록 중...
                        </>
                      ) : (
                        <>
                          <i className="ti ti-building-plus me-2"></i>
                          회사 등록
                        </>
                      )}
                    </button>
                  ) : (
                    <button 
                      type="button" 
                      className="btn btn-lg w-100 text-white fw-semibold mb-4"
                      style={{ 
                        background: 'linear-gradient(135deg, #28a745 0%, #20c997 100%)',
                        border: 'none'
                      }}
                      onClick={() => navigate("/register")}
                    >
                      <i className="ti ti-login me-2"></i>
                      회원가입하러가기
                    </button>
                  )}
                </form>

                <div className="text-center">
                  <span className="text-muted">이미 회사가 등록되어 있나요? </span>
                  <button
                    className="btn btn-link text-decoration-none fw-semibold p-0"
                    style={{ color: '#667eea' }}
                    onClick={() => navigate("/login")}
                  >
                    로그인하기
                  </button>
                </div>

                <div className="mt-4">
                  <h6 className="fw-bold text-muted mb-3" style={{ fontSize: '1rem' }}>
                    <i className="ti ti-lightbulb me-2"></i>
                    안내사항
                  </h6>
                  <ul className="list-unstyled mb-0 text-muted" style={{ fontSize: '0.85rem' }}>
                    <li className="mb-2">• 회사 등록 후 생성된 회사 코드를 직원들과 공유하세요.</li>
                    <li className="mb-2">• 직원들은 회원가입 시 이 코드를 입력해야 합니다.</li>
                    <li>• 회사 코드는 대소문자를 구분하지 않습니다.</li>
                  </ul>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <style jsx="true">{`
        @keyframes float {
          0%, 100% { 
            transform: translateY(0px) rotate(0deg); 
            opacity: 0.7; 
          }
          50% { 
            transform: translateY(-20px) rotate(180deg); 
            opacity: 0.3; 
          }
        }
        
        .form-control:focus {
          border-color: #667eea !important;
          box-shadow: 0 0 0 0.2rem rgba(102, 126, 234, 0.25) !important;
        }
        
        .btn:hover {
          transform: translateY(-2px);
          box-shadow: 0 10px 30px rgba(102, 126, 234, 0.4);
        }
        
        .nav-link:hover {
          color: white !important;
          text-shadow: 0 0 10px rgba(255, 255, 255, 0.5);
        }
      `}</style>
    </div>
  );
};

export default CompanyRegister;