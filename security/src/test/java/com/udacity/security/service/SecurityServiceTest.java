package com.udacity.security.service;

import com.udacity.image.service.ImageService;
import com.udacity.security.service.SecurityService;
import com.udacity.security.application.StatusListener;
import com.udacity.security.data.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SecurityServiceTest {

    private SecurityService securityService;
    private Sensor sensor;
    private final String randomString = UUID.randomUUID().toString();

    /* mock */
    @Mock
    private SecurityRepository securityRepository;

    @Mock
    private ImageService imageService;

    @Mock
    private StatusListener statusListener;

    private Sensor getNewSensor() {
        return new Sensor(randomString, SensorType.DOOR);
    }

    private Set<Sensor> getAllSensors(int count, boolean status) {
        Set<Sensor> sensors = new HashSet<>();
        for (int i = 0; i < count; i ++) {
            sensors.add(new Sensor(randomString, SensorType.DOOR));
        }
        sensors.forEach(sensor -> sensor.setActive(status));
        return sensors;
    }

    @BeforeEach
    void init() {
        securityService = new SecurityService(securityRepository, imageService);
        sensor = getNewSensor();
    }

    // Test 1
    @Test
    void changeAlarmStatus_ifAlarmAndSensorActivate_setAlarmStatusPending() {
        // when is ARMED
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        // when alarm status is NO_ALARM
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);
        securityService.changeSensorActivationStatus(sensor, true);
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }

    // Test 2
    @Test
    void changeAlarmStatus_ifAlarmAndSensorActivateAndPendingAlarm_setAlarmStatusAlarmed() {
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.changeSensorActivationStatus(sensor, true);
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

    //Test 3
    @Test
    void ifAlarmPendingAndSensorsInactive_returnNoAlarmState() {
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        sensor.setActive(false);
        securityService.changeSensorActivationStatus(sensor);
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    // Test 4
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void ifAlarmActiveAndChangeSensor_AlarmStateNotAffected(boolean status) {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
        securityService.changeSensorActivationStatus(sensor, status);
        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }

    // Test 5
    @Test
    void changeAlarmState_ifSensorActivateWhenActiveAndSystemPending_setToAlarmState() {
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
//        sensor.setActive(true);
        securityService.changeSensorActivationStatus(sensor, true);
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

    // Test 6
    @ParameterizedTest
    @EnumSource(value = AlarmStatus.class, names = {"NO_ALARM", "PENDING_ALARM", "ALARM"})
    void checkSensorState_ifSensorDeactivatedWhileInactive_AlarmStateNoChange(AlarmStatus alarmStatus) {
        when(securityRepository.getAlarmStatus()).thenReturn(alarmStatus);
//        sensor.setActive(false);
        securityService.changeSensorActivationStatus(sensor, false);
        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }

    // Test 7
    @Test
    void changeAlarmState_ifCatFoundWhileSystemIsArmed_changeStatusToAlarm() {
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(imageService.imageContainsCat(any(), ArgumentMatchers.anyFloat())).thenReturn(true);
        securityService.processImage(mock(BufferedImage.class));
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

    // Test 8
    @Test
    @MockitoSettings(strictness = Strictness.LENIENT)
    void changeAlarmStatus_CatNotFound_changeStatusToNoAlarm() {
        Set<Sensor> sensors = getAllSensors(3, false);
        when(securityRepository.getSensors()).thenReturn(sensors);
        when(imageService.imageContainsCat(any(), ArgumentMatchers.anyFloat())).thenReturn(false);
        securityService.processImage(mock(BufferedImage.class));
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);

    }

    // Test 9
    @Test
    void changeAlarmStatus_ifSystemDisarm_setStatusNoAlarm() {
        securityService.setArmingStatus(ArmingStatus.DISARMED);
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    // Test 10
    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    void changeSensorState_ifSystemArmed_setToInactive(ArmingStatus armingStatus) {
        Set<Sensor> sensors = getAllSensors(4,true);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        when(securityRepository.getSensors()).thenReturn(sensors);
        securityService.setArmingStatus(armingStatus);

        securityService.getSensors().forEach( sensor -> {
                    assertFalse(sensor.getActive());
        });
    }

    // Test 11
    @Test
    void changeAlarmStatus_ifSystemArmedWhileCatFound_setStatusToAlarm() {
        when(imageService.imageContainsCat(any(), ArgumentMatchers.anyFloat())).thenReturn(true);
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        securityService.processImage(mock(BufferedImage.class));
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

    // Additional Test
    @Test
    void addAndRemoveStatusListener() {
        securityService.addStatusListener(statusListener);
        securityService.removeStatusListener(statusListener);
    }

    @Test
    void addAndRemoveSensor() {
        securityService.addSensor(sensor);
        securityService.removeSensor(sensor);
    }

    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    void changeArmingStatus(ArmingStatus armingStatus) {
        securityService.setArmingStatus(armingStatus);
    }

    @ParameterizedTest
    @EnumSource(value = AlarmStatus.class, names = {"NO_ALARM", "PENDING_ALARM"})
    void changeSensorState_ifSystemDisarmAndSensorOn_armingStateSame(AlarmStatus alarmStatus) {
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.DISARMED);
        when(securityRepository.getAlarmStatus()).thenReturn(alarmStatus);
        securityService.changeSensorActivationStatus(sensor, true);

        verify(securityRepository, never()).setArmingStatus(ArmingStatus.DISARMED);
    }

    @Test
    void changeAlarmStatus_ifSystemDisarmAndAlarmOn_setStatusToPending() {
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.DISARMED);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
        securityService.changeSensorActivationStatus(sensor);
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }

}


