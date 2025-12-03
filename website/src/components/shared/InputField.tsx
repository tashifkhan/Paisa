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
    <div className="absolute left-4 top-1/2 -translate-y-1/2 text-(--muted-foreground)">
      <Icon size={20} />
    </div>
    <input
      type={type}
      value={value}
      onChange={onChange}
      placeholder={placeholder}
      className="w-full bg-(--card) border border-(--border) text-(--foreground) rounded-2xl py-4 pl-12 pr-4 outline-none focus:border-(--primary) focus:ring-2 focus:ring-(--primary)/20 transition-all placeholder-(--muted-foreground)"
    />
  </div>
);
