import React, { useState, useEffect } from 'react';
import { NavigationContainer } from '@react-navigation/native';
import { createNativeStackNavigator } from '@react-navigation/stack';
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import { View, Text, StyleSheet, Platform } from 'react-native';
import * as ScreenOrientation from 'expo-screen-orientation';

// Screens
import ScannerScreen from './screens/ScannerScreen';
import EditorScreen from './screens/EditorScreen';
import QueueScreen from './screens/QueueScreen';
import AuditScreen from './screens/AuditScreen';
import ProfileScreen from './screens/ProfileScreen';
import HomeScreen from './screens/HomeScreen';

// Navigation
const Stack = createNativeStackNavigator();
const Tab = createBottomTabNavigator();

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f5f5f5',
  },
  headerStyle: {
    backgroundColor: '#667eea',
  },
  headerTintColor: '#fff',
  headerTitleStyle: {
    fontWeight: 'bold',
    fontSize: 18,
  },
  tabBarStyle: {
    backgroundColor: '#fff',
    borderTopColor: '#e0e0e0',
    borderTopWidth: 1,
  },
  tabLabelStyle: {
    fontSize: 12,
    fontWeight: '500',
  },
});

function ScannerStack() {
  return (
    <Stack.Navigator
      screenOptions={{
        headerStyle: styles.headerStyle,
        headerTintColor: styles.headerTintColor,
        headerTitleStyle: styles.headerTitleStyle,
      }}
    >
      <Stack.Screen 
        name="ScannerMain" 
        component={ScannerScreen}
        options={{ title: '📸 Scan Receipt' }}
      />
      <Stack.Screen 
        name="Editor" 
        component={EditorScreen}
        options={{ title: '✏️ Edit Receipt' }}
      />
    </Stack.Navigator>
  );
}

function QueueStack() {
  return (
    <Stack.Navigator
      screenOptions={{
        headerStyle: styles.headerStyle,
        headerTintColor: styles.headerTintColor,
        headerTitleStyle: styles.headerTitleStyle,
      }}
    >
      <Stack.Screen 
        name="QueueMain" 
        component={QueueScreen}
        options={{ title: '📋 Transaction Queue' }}
      />
    </Stack.Navigator>
  );
}

function AuditStack() {
  return (
    <Stack.Navigator
      screenOptions={{
        headerStyle: styles.headerStyle,
        headerTintColor: styles.headerTintColor,
        headerTitleStyle: styles.headerTitleStyle,
      }}
    >
      <Stack.Screen 
        name="AuditMain" 
        component={AuditScreen}
        options={{ title: '📊 Audit Trail' }}
      />
    </Stack.Navigator>
  );
}

function ProfileStack() {
  return (
    <Stack.Navigator
      screenOptions={{
        headerStyle: styles.headerStyle,
        headerTintColor: styles.headerTintColor,
        headerTitleStyle: styles.headerTitleStyle,
      }}
    >
      <Stack.Screen 
        name="ProfileMain" 
        component={ProfileScreen}
        options={{ title: '👤 Profile' }}
      />
    </Stack.Navigator>
  );
}

function TabNavigator() {
  return (
    <Tab.Navigator
      screenOptions={{
        tabBarStyle: styles.tabBarStyle,
        tabBarLabelStyle: styles.tabLabelStyle,
        headerShown: false,
      }}
    >
      <Tab.Screen 
        name="Home" 
        component={HomeScreen}
        options={{
          title: 'Home',
          tabBarLabel: 'Home',
          tabBarIcon: ({ color }) => <Text style={{ fontSize: 20, color }}>🏠</Text>,
        }}
      />
      <Tab.Screen 
        name="Scanner" 
        component={ScannerStack}
        options={{
          title: 'Scanner',
          tabBarLabel: 'Scan',
          tabBarIcon: ({ color }) => <Text style={{ fontSize: 20, color }}>📸</Text>,
        }}
      />
      <Tab.Screen 
        name="Queue" 
        component={QueueStack}
        options={{
          title: 'Queue',
          tabBarLabel: 'Queue',
          tabBarIcon: ({ color }) => <Text style={{ fontSize: 20, color }}>📋</Text>,
        }}
      />
      <Tab.Screen 
        name="Audit" 
        component={AuditStack}
        options={{
          title: 'Audit',
          tabBarLabel: 'Audit',
          tabBarIcon: ({ color }) => <Text style={{ fontSize: 20, color }}>📊</Text>,
        }}
      />
      <Tab.Screen 
        name="Profile" 
        component={ProfileStack}
        options={{
          title: 'Profile',
          tabBarLabel: 'Profile',
          tabBarIcon: ({ color }) => <Text style={{ fontSize: 20, color }}>👤</Text>,
        }}
      />
    </Tab.Navigator>
  );
}

export default function App() {
  useEffect(() => {
    // Lock orientation to portrait on mobile
    if (Platform.OS !== 'web') {
      ScreenOrientation.lockAsync(ScreenOrientation.OrientationLock.PORTRAIT);
    }
  }, []);

  return (
    <NavigationContainer>
      <TabNavigator />
    </NavigationContainer>
  );
}
