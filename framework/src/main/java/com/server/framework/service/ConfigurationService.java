package com.server.framework.service;

import com.server.framework.common.DateUtil;
import com.server.framework.entity.ConfigurationEntity;
import com.server.framework.repository.ConfigurationRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
public class ConfigurationService
{

	@Autowired
	private ConfigurationRepository configurationRepository;

	public Optional<String> getValue(String key)
	{
		Optional<ConfigurationEntity> configurationEntityOptional = configurationRepository.findByCKey(key);
		if(configurationEntityOptional.isPresent())
        {
            if(configurationEntityOptional.get().getExpiryTime() != -1 && DateUtil.getCurrentTimeInMillis() > configurationEntityOptional.get().getExpiryTime())
            {
                configurationRepository.delete(configurationEntityOptional.get());
                return Optional.empty();
            }
        }
		return configurationEntityOptional.map(ConfigurationEntity::getCValue);
	}

	public Optional<ConfigurationEntity> get(Long id)
	{
		return configurationRepository.findById(id);
	}

	public void save(ConfigurationEntity configurationEntity)
	{
		configurationRepository.save(configurationEntity);
	}

	public ConfigurationEntity setValue(String key, String value)
	{
		return setValue(key, value, -1L);
	}

	public ConfigurationEntity setValue(String key, String value, Long expiryTime)
	{
		Optional<ConfigurationEntity> existing = configurationRepository.findByCKey(key);
		ConfigurationEntity conf = existing.orElseGet(() -> {
			ConfigurationEntity c = new ConfigurationEntity();
			c.setCKey(key);
			c.setExpiryTime(expiryTime);
			return c;
		});
		conf.setCValue(value);
		conf.setExpiryTime(expiryTime);
		return configurationRepository.save(conf);
	}

	public void delete(String key)
	{
		configurationRepository.deleteByCKey(key);
	}
}
