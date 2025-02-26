import { StyleSheet, View, ViewProps } from 'react-native';
import { useThemeColor } from '@/hooks/useThemeColor';

interface CardProps extends ViewProps {
  variant?: 'primary' | 'secondary';
  elevation?: boolean;
}

export function Card({ style, variant = 'primary', elevation = false, ...props }: CardProps) {
  const backgroundColor = useThemeColor({ light: variant === 'primary' ? '#FFFFFF' : '#F5F5F5', dark: variant === 'primary' ? '#1E1E1E' : '#2D2D2D' });
  const shadowColor = useThemeColor({ light: '#000000', dark: '#FFFFFF' });

  return (
    <View
      style={[
        styles.card,
        { backgroundColor },
        elevation && {
          shadowColor,
          shadowOffset: { width: 0, height: 2 },
          shadowOpacity: 0.1,
          shadowRadius: 8,
          elevation: 3,
        },
        style,
      ]}
      {...props}
    />
  );
}

const styles = StyleSheet.create({
  card: {
    borderRadius: 16,
    padding: 16,
  },
});