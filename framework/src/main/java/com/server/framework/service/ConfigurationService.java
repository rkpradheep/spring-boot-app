package com.server.framework.service;

import com.server.framework.entity.ConfigurationEntity;
import com.server.framework.repository.ConfigurationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
public class ConfigurationService {

    @Autowired
    private ConfigurationRepository configurationRepository;

    public Optional<String> getValue(String key) {
        return configurationRepository.findByCKey(key).map(ConfigurationEntity::getCValue);
    }

    public ConfigurationEntity setValue(String key, String value) {
        Optional<ConfigurationEntity> existing = configurationRepository.findByCKey(key);
        ConfigurationEntity conf = existing.orElseGet(() -> {
            ConfigurationEntity c = new ConfigurationEntity();
            c.setId(generateNextId());
            c.setCKey(key);
            return c;
        });
        conf.setCValue(value);
        return configurationRepository.save(conf);
    }

    public void delete(String key) {
        configurationRepository.deleteByCKey(key);
    }

    private Long generateNextId() {
        return configurationRepository.findAll().stream().map(ConfigurationEntity::getId).filter(id -> id != null).max(Long::compareTo).orElse(0L) + 1;
    }
}
