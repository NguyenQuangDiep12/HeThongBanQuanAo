import { Link } from 'react-router-dom';
import { Heart } from 'lucide-react';
import { money } from '../../api';
import { pics } from '../../shared/media.js';

function isNewProduct(createdAt) {
  if (!createdAt) {
    return false;
  }

  const createdTime = Date.parse(createdAt);

  if (Number.isNaN(createdTime)) {
    return false;
  }

  const sevenDaysInMilliseconds = 7 * 24 * 60 * 60 * 1000;
  const age = Date.now() - createdTime;

  return age >= 0 && age <= sevenDaysInMilliseconds;
}

export function ProductCard({ p }) {
  const isNew = isNewProduct(p.createdAt);

  return (
    <article className="group">
      <Link
        to={'/san-pham/' + p.id}
        className="relative block aspect-[.82] overflow-hidden rounded-2xl bg-slate-100"
      >
        <img
          src={p.imageUrl || pics.fallback}
          onError={event => { event.currentTarget.src = pics.fallback; }}
          alt={p.name}
          className="h-full w-full object-cover transition duration-700 group-hover:scale-105"
        />
        {isNew && (
          <span className="absolute left-3 top-3 rounded-full bg-white/90 px-3 py-1 text-[9px] font-bold uppercase tracking-wider text-brand-800">
            Mới về
          </span>
        )}
        <span className="absolute right-3 top-3 rounded-full bg-white/90 p-2">
          <Heart size={15} />
        </span>
      </Link>
      <p className="mt-3 text-[10px] font-bold uppercase tracking-widest text-slate-400">
        {p.category || 'Thời trang'}
      </p>
      <Link
        to={'/san-pham/' + p.id}
        className="mt-1 block truncate text-sm font-bold hover:text-brand-700"
      >
        {p.name}
      </Link>
      <div className="mt-2 flex justify-between">
        <b className="text-sm">{money(p.price)}</b>
        <small className="text-slate-400">
          {(p.variants || []).some(variant => variant.stockQuantity > 0) ? 'Còn hàng' : 'Hết hàng'}
        </small>
      </div>
    </article>
  );
}
