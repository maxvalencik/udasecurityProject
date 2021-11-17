package com.udacity.catpoint.security.service;


import com.udacity.catpoint.image.service.ImageService;
import com.udacity.catpoint.security.data.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class SecurityServiceTest {

    private Sensor sensor;
    private final String random = UUID.randomUUID().toString();

    @Mock
    private SecurityService securityService;

    @Mock
    private ImageService imageService;

    @Mock
    private SecurityRepository securityRepository;

    private Sensor getNewSensor() {
        return new Sensor(random, SensorType.DOOR);
    }

    private Set<Sensor> getSensors(int count, boolean status) {
        String random = UUID.randomUUID().toString();
        Set<Sensor> sensors = new HashSet<>();
         for (int i = 0; i < count; i++) {
             sensors.add(new Sensor(random, SensorType.DOOR));
         }
         sensors.forEach(it -> it.setActive(status));

         return sensors;
    }

    @BeforeEach
    void settingUp() {
        securityService = new SecurityService(securityRepository, imageService);
        sensor = getNewSensor();
    }


    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    void changeAlarmStatusFromActiveToPending(ArmingStatus armingStatus) {
        when(securityService.getArmingStatus()).thenReturn(armingStatus);
        when(securityService.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);
        securityService.changeSensorActivationStatus(sensor, true);
        verify(securityRepository, atMostOnce()).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }

    @Test
    void changeAlarmStatusFromPendingToAlarm() {
        when(securityService.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.changeSensorActivationStatus(sensor, true);
        verify(securityRepository, atMost(2)).setAlarmStatus(AlarmStatus.ALARM);
    }

    @Test
    void changeAlarmStatusFromPendingToNoAlarm(){
        Set<Sensor> allSensors = getSensors(4, false);
        Sensor lastSensor = allSensors.iterator().next();
        lastSensor.setActive(true);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.changeSensorActivationStatus(lastSensor, false);
        ArgumentCaptor<AlarmStatus> captor = ArgumentCaptor.forClass(AlarmStatus.class);
        verify(securityRepository, atMostOnce()).setAlarmStatus(captor.capture());
        assertEquals(captor.getValue(), AlarmStatus.NO_ALARM);
    }

    @Test
    void alarmStateNotAffectedBySensorDeactivation(){
        securityService.changeSensorActivationStatus(sensor, false);
        verify(securityRepository, never()).setAlarmStatus(any());
    }

    @Test
    void alarmChangeToAlarmWhileActivatedWithAlarmPending(){
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.changeSensorActivationStatus(sensor, true);
        ArgumentCaptor<AlarmStatus> captor = ArgumentCaptor.forClass(AlarmStatus.class);
        verify(securityRepository, atMostOnce()).setAlarmStatus(captor.capture());
        assertEquals(captor.getValue(), AlarmStatus.ALARM);
    }

    @Test
    void changeStatustoNoIfSystemDisarmed(){
        securityService.setArmingStatus(ArmingStatus.DISARMED);
        ArgumentCaptor<AlarmStatus> captor = ArgumentCaptor.forClass(AlarmStatus.class);
        verify(securityRepository, atMostOnce()).setAlarmStatus(captor.capture());
        assertEquals(captor.getValue(), AlarmStatus.NO_ALARM);
    }


    @Test
    void alarmWhenImageHasCats(){
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(true);
        securityService.processImage(mock(BufferedImage.class));
        ArgumentCaptor<AlarmStatus> captor = ArgumentCaptor.forClass(AlarmStatus.class);
        verify(securityRepository, atMostOnce()).setAlarmStatus(captor.capture());
        assertEquals(captor.getValue(), AlarmStatus.ALARM);
    }

    @Test
    void noAlarmIfImageHasNoCats(){
//        Set<Sensor> inactiveSensors = getSensors(4, false);
//        when(securityRepository.getSensors()).thenReturn(inactiveSensors);
        when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(false);
        securityService.processImage(mock(BufferedImage.class));
        ArgumentCaptor<AlarmStatus> captor = ArgumentCaptor.forClass(AlarmStatus.class);
        verify(securityRepository, atMostOnce()).setAlarmStatus(captor.capture());
        assertEquals(captor.getValue(), AlarmStatus.NO_ALARM);
    }
}

