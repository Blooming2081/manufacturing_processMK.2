package com.u1mobis.dashboard_backend.mqtt;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class MQTTSubscriber {

    private final MQTTMessageProcessor messageProcessor;
    private MqttClient mqttClient;

    @PostConstruct
    public void initialize() {
        try {
            mqttClient = new MqttClient("tcp://localhost:1883", "dashboard-backend-" + System.currentTimeMillis());

            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            options.setKeepAliveInterval(30);

            mqttClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    log.error("MQTT 연결 끊어짐: {}", cause.getMessage());
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    String payload = new String(message.getPayload());
                    log.info("MQTT 메시지 수신: topic={}, payload={}", topic, payload);
                    messageProcessor.processMQTTMessage(topic, payload);
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    // 발행 완료 처리 (필요시)
                }
            });

            mqttClient.connect(options);

            // 토픽 구독
            mqttClient.subscribe("factory/+/environment");
            mqttClient.subscribe("factory/+/+/operations");
            mqttClient.subscribe("factory/+/+/production/started");    // 생산 시작 추가
            mqttClient.subscribe("factory/+/+/production/completed");
            mqttClient.subscribe("factory/+/+/conveyor");
            mqttClient.subscribe("factory/+/robot");

            log.info("MQTT 구독 완료 - 브로커: tcp://localhost:1883");

        } catch (Exception e) {
            log.error("MQTT 초기화 실패: {}", e.getMessage());
        }
    }

    @PreDestroy
    public void cleanup() {
        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                mqttClient.disconnect();
                mqttClient.close();
            }
        } catch (Exception e) {
            log.error("MQTT 정리 실패: {}", e.getMessage());
        }
    }
}