import { Link } from "react-router-dom";
import { ArrowRight, Package } from "lucide-react";
import { primary } from "../../shared/uiStyles.js";

export function Empty({
  Icon = Package,
  title,
  body,
  to = "/san-pham",
  action = "Khám phá cửa hàng",
}) {
  return (
    <div className="rounded-3xl border border-dashed border-slate-200 bg-slate-50 p-12 text-center">
      <Icon size={28} className="mx-auto text-brand-700" />
      <h3 className="mt-4 font-display text-lg font-extrabold">{title}</h3>
      <p className="mx-auto mt-2 max-w-sm text-sm leading-6 text-slate-500">
        {body}
      </p>
      <Link className={primary + " mt-5"} to={to}>
        {action}
        <ArrowRight size={15} />
      </Link>
    </div>
  );
}
