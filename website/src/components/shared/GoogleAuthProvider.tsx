import { GoogleOAuthProvider } from "@react-oauth/google";
import type { ReactNode } from "react";

const GOOGLE_CLIENT_ID = import.meta.env.VITE_GOOGLE_CLIENT_ID || "";

interface Props {
	children: ReactNode;
}

export const GoogleAuthProviderWrapper = ({ children }: Props) => {
	if (!GOOGLE_CLIENT_ID) {
		// If no client ID configured, just render children without Google OAuth
		return <>{children}</>;
	}

	return (
		<GoogleOAuthProvider clientId={GOOGLE_CLIENT_ID}>
			{children}
		</GoogleOAuthProvider>
	);
};
