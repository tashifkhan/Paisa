import { ArrowLeft, Users, Mail, Plus, Link as LinkIcon, Copy } from 'lucide-react';
import { InputField } from '../shared/InputField';

interface CreateGroupViewProps {
  setCurrentView: (view: string) => void;
}

export const CreateGroupView = ({ setCurrentView }: CreateGroupViewProps) => (
  <div className="flex flex-col h-full bg-[var(--background)] px-6 pt-12 pb-6 overflow-y-auto">
    <button onClick={() => setCurrentView('debts')} className="absolute top-6 left-6 p-2 bg-[var(--card)] border border-[var(--border)] rounded-full text-[var(--foreground)] shadow-sm">
      <ArrowLeft size={20} />
    </button>

    <div className="flex-1">
      <h1 className="text-3xl font-bold text-[var(--foreground)] mb-6 mt-4">Create Group</h1>

      <div className="space-y-6">
        {/* Group Icon Placeholder */}
        <div className="flex justify-center">
          <div className="w-24 h-24 bg-[var(--muted)] rounded-full flex items-center justify-center border-2 border-dashed border-[var(--border)] text-[var(--muted-foreground)]">
            <Users size={32} />
          </div>
        </div>

        <div>
          <label className="text-sm font-medium text-[var(--foreground)] ml-2 mb-1 block">Group Name</label>
          <InputField icon={Users} type="text" placeholder="e.g. Summer Trip" />
        </div>

        <div>
          <label className="text-sm font-medium text-[var(--foreground)] ml-2 mb-1 block">Invite Members</label>
          <div className="flex gap-2">
            <div className="flex-1">
              <InputField icon={Mail} type="email" placeholder="Enter email address" />
            </div>
            <button className="h-14 w-14 bg-[var(--primary)] text-[var(--primary-foreground)] rounded-2xl flex items-center justify-center shadow-md">
              <Plus size={24} />
            </button>
          </div>
        </div>

        {/* Share Link Option */}
        <div className="bg-[var(--card)] border border-[var(--border)] rounded-2xl p-4">
          <div className="flex items-center gap-3 mb-3">
            <div className="w-10 h-10 bg-[var(--muted)] rounded-full flex items-center justify-center text-[var(--primary)]">
              <LinkIcon size={20} />
            </div>
            <div>
              <h3 className="font-bold text-[var(--foreground)]">Invite via Link</h3>
              <p className="text-xs text-[var(--muted-foreground)]">Share group link with friends</p>
            </div>
          </div>
          <div className="flex gap-2">
            <div className="flex-1 bg-[var(--muted)] rounded-xl px-3 py-3 text-sm text-[var(--muted-foreground)] truncate">
              https://expense.app/invite/gr_9823
            </div>
            <button className="px-4 bg-[var(--foreground)] text-[var(--background)] rounded-xl font-medium text-sm flex items-center gap-1">
              <Copy size={14} /> Copy
            </button>
          </div>
        </div>

        <button onClick={() => setCurrentView('debts')} className="w-full py-4 bg-[var(--primary)] text-[var(--primary-foreground)] rounded-[2rem] font-bold text-lg shadow-lg hover:opacity-90 active:scale-95 transition-all mt-8">
          Create Group
        </button>
      </div>
    </div>
  </div>
);
