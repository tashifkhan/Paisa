import React, { useEffect } from 'react';
import { Pressable, StyleSheet } from 'react-native';
import Animated, { 
  interpolateColor, 
  useAnimatedStyle, 
  useSharedValue, 
  withSpring, 
  withTiming 
} from 'react-native-reanimated';
import { useTheme } from 'react-native-paper';
import { MaterialCommunityIcons } from '@expo/vector-icons';

interface MD3SwitchProps {
  value: boolean;
  onValueChange: (value: boolean) => void;
  disabled?: boolean;
}

const TRACK_WIDTH = 52;
const TRACK_HEIGHT = 32;
const THUMB_SIZE_OFF = 16;
const THUMB_SIZE_ON = 24;
const THUMB_PADDING = 4;

export const MD3Switch: React.FC<MD3SwitchProps> = ({ value, onValueChange, disabled }) => {
  const theme = useTheme();
  const progress = useSharedValue(value ? 1 : 0);

  useEffect(() => {
    progress.value = withSpring(value ? 1 : 0, {
      damping: 20,
      stiffness: 150,
    });
  }, [value]);

  const animatedTrackStyle = useAnimatedStyle(() => {
    return {
      backgroundColor: interpolateColor(
        progress.value,
        [0, 1],
        [theme.colors.surfaceVariant, theme.colors.primary]
      ),
      borderColor: interpolateColor( progress.value, [0, 1], [theme.colors.outline, theme.colors.primary]),
    };
  });

  const animatedThumbStyle = useAnimatedStyle(() => {
    const size = withTiming(value ? THUMB_SIZE_ON : THUMB_SIZE_OFF, { duration: 150 });
    return {
      width: size,
      height: size,
      borderRadius: size / 2,
      backgroundColor: interpolateColor(
        progress.value,
        [0, 1],
        [theme.colors.outline, theme.colors.onPrimary]
      ),
    };
  });

  // Fixed interpolation for translateX
  const thumbTranslateX = useAnimatedStyle(() => {
    const margin = THUMB_PADDING;
    const start = margin + (THUMB_SIZE_ON - THUMB_SIZE_OFF) / 2 - (THUMB_SIZE_ON - THUMB_SIZE_OFF) / 2; // complicates things
    
    // Simplest: Track is 52. Thumb is 32 high. Padding 4.
    // Thumb center when off: X=16. 
    // Thumb center when on: X=36.
    
    const availableSpace = TRACK_WIDTH - THUMB_PADDING * 2;
    const offPosition = THUMB_PADDING + (THUMB_SIZE_ON - THUMB_SIZE_OFF) / 2;
    const onPosition = TRACK_WIDTH - THUMB_PADDING - THUMB_SIZE_ON;
    
    // Actually, MD3 spec:
    // Off: Thumb 16x16, centered at X=16. (4+12+4? no)
    // Detailed:
    // Track 52 wide. 
    // Off thumb: 16 wide. Positioned such that the gap on left is equal to gap on top? 
    // Usually Y centered.
    
    const xPos = progress.value * (TRACK_WIDTH - THUMB_SIZE_ON - THUMB_PADDING * 2) + THUMB_PADDING;

    return {
      transform: [{ translateX: xPos }],
      width: withTiming(value ? THUMB_SIZE_ON : THUMB_SIZE_OFF, { duration: 150 }),
      height: withTiming(value ? THUMB_SIZE_ON : THUMB_SIZE_OFF, { duration: 150 }),
      backgroundColor: interpolateColor(
        progress.value,
        [0, 1],
        [theme.colors.outline, theme.colors.onPrimary]
      ),
    };
  });

  const iconOpacity = useAnimatedStyle(() => {
    return {
      opacity: withTiming(value ? 1 : 0, { duration: 150 }),
    };
  });

  return (
    <Pressable 
      onPress={() => !disabled && onValueChange(!value)}
      disabled={disabled}
      style={{ opacity: disabled ? 0.5 : 1 }}
    >
      <Animated.View style={[styles.track, animatedTrackStyle]}>
        <Animated.View style={[styles.thumb, thumbTranslateX]}>
          <Animated.View style={[styles.iconContainer, iconOpacity]}>
            <MaterialCommunityIcons 
              name="check" 
              size={16} 
              color={theme.colors.primary} 
            />
          </Animated.View>
        </Animated.View>
      </Animated.View>
    </Pressable>
  );
};

const styles = StyleSheet.create({
  track: {
    width: TRACK_WIDTH,
    height: TRACK_HEIGHT,
    borderRadius: TRACK_HEIGHT / 2,
    borderWidth: 0, // MD3 can have a border in some states but usually solid
    justifyContent: 'center',
  },
  thumb: {
    position: 'absolute',
    borderRadius: 12,
    alignItems: 'center',
    justifyContent: 'center',
  },
  iconContainer: {
    width: '100%',
    height: '100%',
    alignItems: 'center',
    justifyContent: 'center',
  }
});
