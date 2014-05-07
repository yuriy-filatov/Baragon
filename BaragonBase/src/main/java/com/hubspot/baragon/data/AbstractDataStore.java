package com.hubspot.baragon.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.io.BaseEncoding;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.PathAndBytesable;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

// because curator is a piece of shit
public abstract class AbstractDataStore {
  protected final CuratorFramework curatorFramework;
  protected final ObjectMapper objectMapper;

  public static final Comparator<String> SEQUENCE_NODE_COMPARATOR_LOW_TO_HIGH = new Comparator<String>() {
    @Override
    public int compare(String o1, String o2) {
      return o1.substring(o1.length()-10).compareTo(o2.substring(o2.length()-10));
    }
  };

  public static final Comparator<String> SEQUENCE_NODE_COMPARATOR_HIGH_TO_LOW = new Comparator<String>() {
    @Override
    public int compare(String o1, String o2) {
      return o2.substring(o2.length()-10).compareTo(o1.substring(o1.length()-10));
    }
  };

  public AbstractDataStore(CuratorFramework curatorFramework, ObjectMapper objectMapper) {
    this.curatorFramework = curatorFramework;
    this.objectMapper = objectMapper;
  }

  protected String encodeUrl(String url) {
    return BaseEncoding.base64Url().encode(url.getBytes());
  }

  protected boolean nodeExists(String path) {
    try {
      return curatorFramework.checkExists().forPath(path) != null;
    } catch (KeeperException.NoNodeException e) {
      return false;
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  protected <T> void writeToZk(String path, T data) {
    try {
      final byte[] serializedInfo = objectMapper.writeValueAsBytes(data);

      final PathAndBytesable<?> builder;

      if (curatorFramework.checkExists().forPath(path) != null) {
        builder = curatorFramework.setData();
      } else {
        builder = curatorFramework.create().creatingParentsIfNeeded();
      }

      builder.forPath(path, serializedInfo);
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  protected <T> Optional<T> readFromZk(String path, Class<T> klass) {
    try {
      return Optional.of(objectMapper.readValue(curatorFramework.getData().forPath(path), klass));
    } catch (KeeperException.NoNodeException nne) {
      return Optional.absent();
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  protected String createNode(String path) {
    try {
      return curatorFramework.create().creatingParentsIfNeeded().forPath(path);
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  protected String createPersistentSequentialNode(String path) {
    try {
      return curatorFramework.create().creatingParentsIfNeeded().withMode(CreateMode.PERSISTENT_SEQUENTIAL).forPath(path);
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  protected <T> String createPersistentSequentialNode(String path, T value) {
    try {
      final byte[] serializedValue = objectMapper.writeValueAsBytes(value);

      return curatorFramework.create().creatingParentsIfNeeded().withMode(CreateMode.PERSISTENT_SEQUENTIAL).forPath(path, serializedValue);
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }

  }

  protected boolean deleteNode(String path) {
    try {
      curatorFramework.delete().forPath(path);
      return true;
    } catch (KeeperException.NoNodeException e) {
      return false;
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  protected List<String> getChildren(String path) {
    try {
      return curatorFramework.getChildren().forPath(path);
    } catch (KeeperException.NoNodeException e) {
      return Collections.emptyList();
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }
}