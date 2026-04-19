import { Stack } from 'expo-router';
import React from 'react';

export default function ModalsLayout() {
  return (
    <Stack screenOptions={{ headerShown: false }}>
      <Stack.Screen name="add-expense" options={{ presentation: 'modal' }} />
      <Stack.Screen name="add-wallet" options={{ presentation: 'modal' }} />
      <Stack.Screen name="add-debt" options={{ presentation: 'modal' }} />
      <Stack.Screen name="create-group" options={{ presentation: 'modal' }} />
      <Stack.Screen
        name="transaction/[id]"
        options={{ presentation: 'transparentModal', animation: 'fade' }}
      />
    </Stack>
  );
}
