import { ArrowLeft, Plane, Share2, MoreHorizontal, Plus, MoveUpRight, RefreshCw } from 'lucide-react';
import { Home, Pizza, Banknote } from 'lucide-react';

interface GroupExpense {
  id: number;
  title: string;
  amount: number;
  paidBy: string;
  date: string;
  icon: any;
}

interface GroupDetailViewProps {
  groupExpenses: GroupExpense[];
  groupDetailTab: string;
  setGroupDetailTab: (tab: string) => void;
  setCurrentView: (view: string) => void;
}

export const GroupDetailView = ({ groupExpenses, groupDetailTab, setGroupDetailTab, setCurrentView }: GroupDetailViewProps) => (
  <div className="flex flex-col h-full bg-(--background) overflow-y-auto hide-scrollbar transition-colors duration-300">
    {/* Header with Group Info */}
    <div className="bg-(--card) rounded-b-[3rem] shadow-sm border-b border-(--border) pb-6">
      <header className="flex justify-between items-center p-6">
        <button onClick={() => setCurrentView('debts')} className="p-2 bg-(--muted) rounded-full text-(--foreground)">
          <ArrowLeft size={20} />
        </button>
        <div className="flex items-center gap-2">
          <button className="p-2 text-(--foreground)"><Share2 size={20} /></button>
          <button className="p-2 text-(--foreground)"><MoreHorizontal size={20} /></button>
        </div>
      </header>

      <div className="px-6 text-center">
        <div className="w-20 h-20 bg-orange-500 rounded-3xl mx-auto flex items-center justify-center text-white shadow-lg mb-4 rotate-3">
          <Plane size={32} />
        </div>
        <h1 className="text-2xl font-bold text-(--foreground)">Goa Trip</h1>
        <p className="text-(--muted-foreground) text-sm mb-6">5 members • Created on 12 Jan</p>

        <div className="bg-(--muted) rounded-2xl p-4 inline-flex items-center gap-4 border border-(--border)">
          <div className="text-left">
            <div className="text-xs text-(--muted-foreground)">Total Expenses</div>
            <div className="text-lg font-bold text-(--foreground)">₹45,200</div>
          </div>
          <div className="h-8 w-px bg-(--border)"></div>
          <div className="text-left">
            <div className="text-xs text-(--muted-foreground)">You Owe</div>
            <div className="text-lg font-bold text-red-500">₹2,000</div>
          </div>
        </div>
      </div>

      {/* Inner Tabs */}
      <div className="px-6 mt-6">
        <div className="flex justify-between items-center bg-(--muted) rounded-[2rem] p-1 text-sm font-medium">
          {['Expenses', 'Balances', 'Settings'].map(tab => (
            <button
              key={tab}
              onClick={() => setGroupDetailTab(tab.toLowerCase())}
              className={`flex-1 py-3 rounded-[2rem] transition-all ${groupDetailTab === tab.toLowerCase() ? 'bg-(--primary) text-(--primary-foreground) shadow-md' : 'text-(--muted-foreground)'}`}
            >
              {tab}
            </button>
          ))}
        </div>
      </div>
    </div>

    <div className="flex-1 px-6 pt-6 pb-24">
      {groupDetailTab === 'expenses' && (
        <div className="space-y-1">
          {groupExpenses.map(exp => (
            <div key={exp.id} className="flex items-center justify-between py-4 border-b border-(--border) last:border-0">
              <div className="flex items-center gap-4">
                <div className="w-10 h-10 bg-(--muted) rounded-full flex items-center justify-center text-(--foreground)">
                  <exp.icon size={18} />
                </div>
                <div>
                  <div className="font-bold text-(--foreground)">{exp.title}</div>
                  <div className="text-xs text-(--muted-foreground)">{exp.paidBy} paid • {exp.date}</div>
                </div>
              </div>
              <div className="text-right">
                <div className="font-bold text-(--foreground)">₹{exp.amount}</div>
                <div className="text-xs text-(--primary) font-medium">You lent ₹{exp.amount / 5}</div>
              </div>
            </div>
          ))}
        </div>
      )}

      {groupDetailTab === 'balances' && (
        <div className="space-y-4">
          <div className="bg-(--card) border border-(--border) rounded-2xl p-4 flex items-center justify-between">
            <div className="flex items-center gap-3">
              <div className="w-8 h-8 rounded-full bg-red-100 text-red-600 flex items-center justify-center text-xs font-bold">You</div>
              <MoveUpRight size={16} className="text-(--muted-foreground)" />
              <div className="w-8 h-8 rounded-full bg-blue-100 text-blue-600 flex items-center justify-center text-xs font-bold">RS</div>
            </div>
            <div className="text-sm font-medium text-(--foreground)">Pay Rahul Sharma <span className="font-bold">₹2,000</span></div>
          </div>

          <div className="bg-(--muted) rounded-2xl p-6 text-center mt-8">
            <RefreshCw size={32} className="mx-auto text-(--muted-foreground) mb-3" />
            <h3 className="font-bold text-(--foreground)">Simplify Debts?</h3>
            <p className="text-xs text-(--muted-foreground) mb-4">Minimize the number of transactions required to settle up.</p>
            <button className="px-6 py-2 bg-(--foreground) text-(--background) rounded-xl text-sm font-bold">Simplify Now</button>
          </div>
        </div>
      )}
    </div>

    {/* Floating Add for Group */}
    <div className="absolute bottom-6 right-6">
      <button onClick={() => setCurrentView('addExpense')} className="h-14 w-14 bg-(--primary) text-(--primary-foreground) rounded-full shadow-lg flex items-center justify-center">
        <Plus size={24} />
      </button>
    </div>
  </div>
);
