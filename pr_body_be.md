## Summary
---
- 마이페이지 내 거래 내역(판매/구매) 전체 조회 API 추가 (`GET /api/cmypage/trades`)
- 마이페이지 내 관심목록(찜한 상품) 전체 조회 API 추가 (`GET /api/cmypage/wishlist`)
- `CmypageMapper` 내 `PRODUCT` 및 `TRADE_CHAT_ROOM`, `PRODUCT_LIKE` 조인 쿼리 구현
- 응답용 DTO `CmypageTradeDto`, `CmypageWishlistDto` 신규 생성
