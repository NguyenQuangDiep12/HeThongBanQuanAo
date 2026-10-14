# Frontend — chạy độc lập

Mở PowerShell riêng cho frontend:

```powershell
cd C:\DAAAN\shopee\frontend
pnpm install
pnpm dev
```

Mở http://localhost:5173. Frontend gọi backend qua Vite proxy `/api`; nếu cần dữ liệu từ server thì chạy backend riêng tại http://localhost:8082.

## Clean/build frontend

```powershell
cd C:\DAAAN\shopee\frontend
pnpm run clean
pnpm run build
```

Frontend không dùng package/script điều phối từ thư mục cha.

## Deploy frontend trên Vercel

Chọn thư mục `frontend` làm Root Directory, dùng lệnh build `pnpm run build` và thư mục output `dist`. Khai báo biến build `VITE_API_BASE_URL` bằng origin backend đang chạy, ví dụ `https://api.example.com` (không thêm `/api`). Backend phải cho phép origin frontend qua `APP_CORS_ALLOWED_ORIGINS` và phải kết nối được tới database có catalog. Vercel chỉ phục vụ frontend tĩnh; việc nạp catalog được backend thực hiện khi khởi động, không phải do Vercel chạy.
