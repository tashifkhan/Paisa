import React from 'react';
import { View, Pressable, StyleProp, ViewStyle } from 'react-native';
import { Text, useTheme } from 'react-native-paper';

export interface TabItem {
  value: string;
  label: string;
}

export interface CustomSegmentedTabsProps {
  tabs: TabItem[];
  value: string;
  onValueChange: (value: string) => void;
  containerStyle?: StyleProp<ViewStyle>;
}

export const CustomSegmentedTabs: React.FC<CustomSegmentedTabsProps> = ({ 
  tabs, 
  value, 
  onValueChange, 
  containerStyle 
}) => {
  const theme = useTheme();
  
  return (
    <View style={containerStyle}>
      <View style={{ 
        flexDirection: 'row', 
        backgroundColor: theme.colors.surface, 
        borderRadius: 24,
        padding: 6 
      }}>
        {tabs.map(t => {
          const isSelected = value === t.value;
          return (
            <Pressable 
              key={t.value}
              style={{ 
                flex: 1, 
                paddingVertical: 12, 
                alignItems: 'center',
                backgroundColor: isSelected ? theme.colors.primaryContainer : 'transparent',
                borderRadius: 20
              }}
              onPress={() => onValueChange(t.value)}
            >
              <Text style={{ 
                fontWeight: isSelected ? '700' : '600', 
                fontSize: 12,
                color: isSelected ? theme.colors.onPrimaryContainer : theme.colors.onSurfaceVariant 
              }}>{t.label}</Text>
            </Pressable>
          );
        })}
      </View>
    </View>
  );
};
