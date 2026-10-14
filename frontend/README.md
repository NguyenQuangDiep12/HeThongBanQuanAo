# Frontend — chạy độc lập

Mở PowerShell riêng cho frontend:

```powershell
cd C:\DAAAN\shopee\frontend
npm install
npm run dev
```

Mở http://localhost:5173. Frontend gọi backend qua Vite proxy `/api`; nếu cần dữ liệu từ server thì chạy backend riêng tại http://localhost:8080.

## Clean/build frontend

```powershell
cd C:\DAAAN\shopee\frontend
npm run clean
npm run build
```

Frontend không dùng package/script điều phối từ thư mục cha.
