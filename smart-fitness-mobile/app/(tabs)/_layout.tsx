import { Ionicons } from '@expo/vector-icons';
import { Tabs } from 'expo-router';
import { colors } from '@/src/theme';

export default function TabsLayout() {
  return (
    <Tabs
      screenOptions={{
        headerStyle: { backgroundColor: colors.card },
        headerTintColor: colors.text,
        headerShadowVisible: false,
        tabBarStyle: { backgroundColor: colors.card, borderTopColor: colors.border },
        tabBarActiveTintColor: colors.accent,
        tabBarInactiveTintColor: colors.muted,
      }}
    >
      <Tabs.Screen
        name="coach"
        options={{
          title: '教练',
          tabBarIcon: ({ color, size }) => <Ionicons name="chatbubble-ellipses" color={color} size={size} />,
        }}
      />
      <Tabs.Screen
        name="training"
        options={{
          title: '训练',
          tabBarIcon: ({ color, size }) => <Ionicons name="barbell" color={color} size={size} />,
        }}
      />
      <Tabs.Screen
        name="body"
        options={{
          title: '身体',
          tabBarIcon: ({ color, size }) => <Ionicons name="body" color={color} size={size} />,
        }}
      />
      <Tabs.Screen
        name="profile"
        options={{
          title: '我的',
          tabBarIcon: ({ color, size }) => <Ionicons name="person" color={color} size={size} />,
        }}
      />
    </Tabs>
  );
}
