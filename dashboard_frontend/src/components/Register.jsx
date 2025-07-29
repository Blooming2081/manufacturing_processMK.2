import React, { useState } from 'react';
import { useNavigate } from "react-router-dom";
import apiService from "../service/apiService";

const Register = () => {
  const [form, setForm] = useState({
    employeeCode: '',
    name: '',
    username: '',
    password: '',
    confirmPassword: '',
    email: '',
    birthNumber: '',
    companyCode: '',
  });
  
  const navigate = useNavigate();

  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleChange = (e) => {
    const { name, value } = e.target;
    
    // 생년월일 입력 처리 (숫자만 허용, 최대 6자리)
    if (name === 'birthNumber') {
      const numericValue = value.replace(/[^0-9]/g, '');
      if (numericValue.length <= 6) {
        setForm(prev => ({ ...prev, [name]: numericValue }));
      }
      return;
    }
    
    setForm(prev => ({ ...prev, [name]: value }));
  };

  // 주민등록번호 앞 6자리를 YYYY-MM-DD 형태로 변환
  const formatBirthNumber = (birthNumber) => {
    if (birthNumber.length !== 6) return null;
    
    const year = birthNumber.substring(0, 2);
    const month = birthNumber.substring(2, 4);
    const day = birthNumber.substring(4, 6);
    
    // 2000년 이후는 00-29, 1900년대는 30-99로 가정
    const fullYear = parseInt(year) <= 29 ? `20${year}` : `19${year}`;
    
    // 유효한 날짜인지 확인
    const date = new Date(fullYear, parseInt(month) - 1, parseInt(day));
    if (date.getFullYear() != fullYear || 
        date.getMonth() != parseInt(month) - 1 || 
        date.getDate() != parseInt(day)) {
      return null;
    }
    
    return `${fullYear}-${month.padStart(2, '0')}-${day.padStart(2, '0')}`;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (form.password !== form.confirmPassword) {
      setError('비밀번호가 일치하지 않습니다.');
      return;
    }

    // 생년월일 유효성 검사
    const formattedBirth = formatBirthNumber(form.birthNumber);
    if (!formattedBirth) {
      setError('생년월일을 올바르게 입력해주세요. (예: 901225)');
      return;
    }

    setError('');
    setLoading(true);

    try {
      const registerData = {
        username: form.username,
        password: form.password,
        email: form.email,
        birth: formattedBirth, // 변환된 날짜 형태로 전송
        employeeCode: form.employeeCode,
        name: form.name,
        companyCode: form.companyCode
      };

      const response = await apiService.user.register(registerData);

      if (response.success) {
        localStorage.setItem('accessToken', response.accessToken);
        localStorage.setItem('refreshToken', response.refreshToken);
        
        const userData = {
          userName: response.userName,
          companyName: response.companyName,
          userId: response.userId,
          companyId: response.companyId
        };
        localStorage.setItem('userData', JSON.stringify(userData));
        localStorage.setItem('isLoggedIn', 'true');
        
        alert('회원가입이 완료되었습니다!');
        navigate(`/${response.companyName}/dashboard`);
      } else {
        setError(response.message || '회원가입에 실패했습니다.');
      }
    } catch (error) {
      console.error('Registration error:', error);
      setError('서버와의 통신에 실패했습니다.');
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
          <div className="col-lg-8 col-xl-6">
            <div className="card border-0 shadow-lg" 
                 style={{ 
                   background: 'rgba(255, 255, 255, 0.95)',
                   backdropFilter: 'blur(20px)',
                   borderRadius: '24px'
                 }}>
              <div className="card-body p-5">
                <div className="text-center mb-4">
                  <div className="mb-3">
                    <i className="ti ti-user-plus fs-1 text-primary"></i>
                  </div>
                  <h2 className="card-title fw-bold fs-2 mb-2">회원가입</h2>
                  <p className="text-muted">환영합니다! 아래 정보를 입력해주세요.</p>
                </div>

                <form onSubmit={handleSubmit}>
                  <div className="row">
                    <div className="col-md-6 mb-3">
                      <label className="form-label fw-semibold mb-2">
                        <i className="ti ti-id me-2"></i>
                        사원코드
                      </label>
                      <input
                        type="text"
                        className="form-control form-control-lg border-2"
                        name="employeeCode"
                        value={form.employeeCode}
                        onChange={handleChange}
                        placeholder="사원코드를 입력하세요"
                        required
                        style={{ borderColor: '#e5e7eb', fontSize: '0.9rem' }}
                      />
                    </div>

                    <div className="col-md-6 mb-3">
                      <label className="form-label fw-semibold mb-2">
                        <i className="ti ti-user me-2"></i>
                        이름
                      </label>
                      <input
                        type="text"
                        className="form-control form-control-lg border-2"
                        name="name"
                        value={form.name}
                        onChange={handleChange}
                        placeholder="이름을 입력하세요"
                        required
                        style={{ borderColor: '#e5e7eb', fontSize: '0.9rem' }}
                      />
                    </div>
                  </div>

                  <div className="row">
                    <div className="col-md-6 mb-3">
                      <label className="form-label fw-semibold mb-2">
                        <i className="ti ti-user-circle me-2"></i>
                        사용할 ID
                      </label>
                      <input
                        type="text"
                        className="form-control form-control-lg border-2"
                        name="username"
                        value={form.username}
                        onChange={handleChange}
                        placeholder="ID를 입력하세요"
                        required
                        style={{ borderColor: '#e5e7eb', fontSize: '0.9rem' }}
                      />
                    </div>

                    <div className="col-md-6 mb-3">
                      <label className="form-label fw-semibold mb-2">
                        <i className="ti ti-mail me-2"></i>
                        이메일
                      </label>
                      <input
                        type="email"
                        className="form-control form-control-lg border-2"
                        name="email"
                        value={form.email}
                        onChange={handleChange}
                        placeholder="이메일을 입력하세요"
                        required
                        style={{ borderColor: '#e5e7eb', fontSize: '0.9rem' }}
                      />
                    </div>
                  </div>

                  <div className="row">
                    <div className="col-md-6 mb-3">
                      <label className="form-label fw-semibold mb-2">
                        <i className="ti ti-lock me-2"></i>
                        사용할 PW
                      </label>
                      <input
                        type="password"
                        className="form-control form-control-lg border-2"
                        name="password"
                        value={form.password}
                        onChange={handleChange}
                        placeholder="비밀번호를 입력하세요"
                        required
                        style={{ borderColor: '#e5e7eb', fontSize: '0.9rem' }}
                      />
                    </div>

                    <div className="col-md-6 mb-3">
                      <label className="form-label fw-semibold mb-2">
                        <i className="ti ti-lock-check me-2"></i>
                        PW 확인
                      </label>
                      <input
                        type="password"
                        className="form-control form-control-lg border-2"
                        name="confirmPassword"
                        value={form.confirmPassword}
                        onChange={handleChange}
                        placeholder="비밀번호를 다시 입력하세요"
                        required
                        style={{ borderColor: '#e5e7eb', fontSize: '0.9rem' }}
                      />
                    </div>
                  </div>

                  <div className="row">
                    <div className="col-md-6 mb-3">
                      <label className="form-label fw-semibold mb-2">
                        <i className="ti ti-calendar me-2"></i>
                        생년월일
                      </label>
                      <input
                        type="text"
                        className="form-control form-control-lg border-2"
                        name="birthNumber"
                        value={form.birthNumber}
                        onChange={handleChange}
                        placeholder="주민번호 앞 6자리 (예: 901225)"
                        maxLength="6"
                        required
                        style={{ borderColor: '#e5e7eb', fontSize: '0.9rem' }}
                      />
                      <div className="form-text">
                        <i className="ti ti-info-circle me-1"></i>
                        주민등록번호 앞 6자리를 입력하세요 (YYMMDD)
                      </div>
                    </div>

                    <div className="col-md-6 mb-4">
                      <label className="form-label fw-semibold mb-2">
                        <i className="ti ti-building me-2"></i>
                        회사 코드
                      </label>
                      <input
                        type="text"
                        className="form-control form-control-lg border-2"
                        name="companyCode"
                        value={form.companyCode}
                        onChange={handleChange}
                        placeholder="회사 코드를 입력하세요"
                        required
                        style={{ borderColor: '#e5e7eb', fontSize: '0.9rem' }}
                      />
                      <div className="form-text">
                        <i className="ti ti-info-circle me-1"></i>
                        회사 관리자로부터 받은 회사 코드를 입력하세요.
                      </div>
                    </div>
                  </div>

                  {error && (
                    <div className="alert alert-danger d-flex align-items-center py-3 mb-4">
                      <i className="ti ti-alert-circle me-2"></i>
                      {error}
                    </div>
                  )}

                  <button 
                    type="submit" 
                    className="btn btn-lg w-100 text-white fw-semibold mb-4"
                    style={{ 
                      background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
                      border: 'none'
                    }}
                    disabled={loading}
                  >
                    {loading ? (
                      <>
                        <span className="spinner-border spinner-border-sm me-2" role="status"></span>
                        회원가입 중...
                      </>
                    ) : (
                      <>
                        <i className="ti ti-user-plus me-2"></i>
                        회원가입
                      </>
                    )}
                  </button>
                </form>

                <div className="text-center">
                  <span className="text-muted">이미 계정이 있으신가요? </span>
                  <button
                    className="btn btn-link text-decoration-none fw-semibold p-0"
                    style={{ color: '#667eea' }}
                    onClick={() => navigate("/login")}
                  >
                    로그인하기
                  </button>
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
      `}</style>
    </div>
  );
};

export default Register;
