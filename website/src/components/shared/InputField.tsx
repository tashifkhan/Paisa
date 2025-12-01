import { type LucideIcon } from 'lucide-react';

interface InputFieldProps {
  icon: LucideIcon;
  type: string;
  placeholder: string;
  value?: string;
  onChange?: (e: React.ChangeEvent<HTMLInputElement>) => void;
}

export const InputField = ({ icon: Icon, type, placeholder, value, onChange }: InputFieldProps) => (
  <div className="relative w-full mb-4">
    <div className="absolute left-4 top-1/2 -translate-y-1/2 text-[var(--muted-foreground)]">
      <Icon size={20} />
    </div>
    <input
      type={type}
      value={value}
      onChange={onChange}
      placeholder={placeholder}
      className="w-full bg-[var(--card)] border border-[var(--border)] text-[var(--foreground)] rounded-2xl py-4 pl-12 pr-4 outline-none focus:border-[var(--primary)] focus:ring-2 focus:ring-[var(--primary)]/20 transition-all placeholder-[var(--muted-foreground)]"
    />
  </div>
);
