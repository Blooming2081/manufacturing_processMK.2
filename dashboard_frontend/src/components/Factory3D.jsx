import Factory3DTwin from "./Twin/Factory3DTwin";
import Navbar from "./Twin/Navabr";

const Factory3D = () => {
  return (
    <div className="row g-3">
      <div className="col-sm-6 col-lg-4">
        <div className="card">
          <div className="card-body" style={{ height: "50rem" }}>
            <Navbar />
            {/* 왼쪽 네비게이션 및 컨트롤 패널 */}
          </div>
        </div>
      </div>

      <div className="col-sm-6 col-lg-8">
        <div className="card">
          <div className="card-body" style={{ height: "50rem" }}>
            <Factory3DTwin />
          </div>
        </div>
      </div>
    </div>
  );
};

export default Factory3D;