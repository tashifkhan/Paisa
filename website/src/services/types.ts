// Backend API Types

export interface BackendTransaction {
    id: string;
    user_id: string;
    wallet_id?: string;
    group_id?: string;
    category_id?: string;
    amount: number;
    currency: string;
    type: string;
    date: string; // ISO string
    note?: string;
}

export interface BackendGroup {
    id: string;
    name: string;
    base_currency: string;
    created_by_user_id?: string;
    icon?: string;
    color?: string;
}

export interface BackendWallet {
    id: string;
    name: string;
    type?: string;
    balance: number;
    currency: string;
}

export interface BackendCategory {
    id: string;
    name: string;
    icon?: string;
    color?: string;
    type: 'expense' | 'income' | 'both';
    is_default: boolean;
}

export interface BackendDebt {
    id: string;
    counterparty_name: string;
    amount: number;
    type: 'owed_to_me' | 'owed_by_me';
    due_date?: string;
}

export interface BackendUser {
    id: string;
    email: string;
    name?: string;
    currency?: string;
    language?: string;
}

export interface BackendGroupMember {
    id: string;
    user_id: string;
    role: string;
    joined_at: string;
    user_name?: string;
    user_email?: string;
}

export interface BackendUserBalance {
    user_id: string;
    user_name?: string;
    balance: number;
}

export interface BackendGroupBalanceSummary {
    group_id: string;
    group_name: string;
    total_expenses: number;
    balances: BackendUserBalance[];
}

export interface BackendStatsSummary {
    period_days: number;
    totals: Record<string, number>;
}

export interface BackendCategoryStats {
    category_id?: string;
    category_name: string;
    total: number;
    percentage: number;
    count: number;
}

export interface BackendFullStats {
    period_days: number;
    total_expense: number;
    total_income: number;
    net: number;
    by_category: BackendCategoryStats[];
}

export interface BackendTrend {
    month: string;
    month_name: string;
    income: number;
    expense: number;
    net: number;
}

export interface BackendComparison {
    period_days: number;
    income: {
        current: number;
        previous: number;
        change: number;
        change_percent: number;
    };
    expense: {
        current: number;
        previous: number;
        change: number;
        change_percent: number;
    };
    net: {
        current: number;
        previous: number;
    };
}

export interface BackendDebtSummary {
    owed_to_me: number;
    owed_by_me: number;
    net: number;
}
