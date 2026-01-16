import api from "../lib/api";
import type {
    BackendGroup,
    BackendGroupBalanceSummary,
    BackendGroupMember,
    BackendSimplifyDebtsResponse,
    BackendTransaction,
} from "./types";

export interface CreateGroupData {
	name: string;
	base_currency?: string;
	icon?: string;
	color?: string;
}

export interface AddMemberData {
	user_id: string;
	role?: "admin" | "member";
}

export const groupService = {
	// Group CRUD
	async getGroups(): Promise<BackendGroup[]> {
		const response = await api.get<BackendGroup[]>("/groups/");
		return response.data;
	},

	async getGroup(id: string): Promise<BackendGroup> {
		const response = await api.get<BackendGroup>(`/groups/${id}`);
		return response.data;
	},

	async createGroup(
		data: CreateGroupData,
	): Promise<{ status: string; id: string }> {
		const response = await api.post("/groups/", data);
		return response.data;
	},

	async updateGroup(
		id: string,
		data: Partial<CreateGroupData>,
	): Promise<BackendGroup> {
		const response = await api.put<BackendGroup>(`/groups/${id}`, data);
		return response.data;
	},

	async deleteGroup(id: string): Promise<{ status: string }> {
		const response = await api.delete(`/groups/${id}`);
		return response.data;
	},

	// Member Management
	async getMembers(groupId: string): Promise<BackendGroupMember[]> {
		const response = await api.get<BackendGroupMember[]>(
			`/groups/${groupId}/members`,
		);
		return response.data;
	},

	async addMember(
		groupId: string,
		data: AddMemberData,
	): Promise<BackendGroupMember> {
		const response = await api.post<BackendGroupMember>(
			`/groups/${groupId}/members`,
			data,
		);
		return response.data;
	},

	async removeMember(
		groupId: string,
		userId: string,
	): Promise<{ status: string }> {
		const response = await api.delete(`/groups/${groupId}/members/${userId}`);
		return response.data;
	},

	// Group Expenses
	async getGroupExpenses(groupId: string): Promise<BackendTransaction[]> {
		const response = await api.get<BackendTransaction[]>(
			`/groups/${groupId}/expenses`,
		);
		return response.data;
	},

	async addGroupExpense(
		groupId: string,
		data: {
			amount: number;
			currency?: string;
			type?: string;
			date?: string;
			note?: string;
			category_id?: string;
			split_strategy?: string;
		},
	): Promise<{ status: string; id: string }> {
		const response = await api.post(`/groups/${groupId}/expenses`, data);
		return response.data;
	},

	// Group Balances
	async getGroupBalances(groupId: string): Promise<BackendGroupBalanceSummary> {
		const response = await api.get<BackendGroupBalanceSummary>(
			`/groups/${groupId}/balances`,
		);
		return response.data;
	},

	// Simplify Debts
	async simplifyDebts(groupId: string): Promise<BackendSimplifyDebtsResponse> {
		const response = await api.post<BackendSimplifyDebtsResponse>(
			`/groups/${groupId}/simplify-debts`,
		);
		return response.data;
	},
};
