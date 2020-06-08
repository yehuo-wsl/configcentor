package curator;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.*;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.junit.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by VULCAN on 2018/11/5.
 */
public class EventTest {

    //ZooKeeper服务地址
    private static final String SERVER = "192.168.30.10:2181";

    //会话超时时间
    private final int SESSION_TIMEOUT = 30000;

    //连接超时时间
    private final int CONNECTION_TIMEOUT = 5000;

    //创建连接实例
    private CuratorFramework client = null;

    /**
     * baseSleepTimeMs：初始的重试等待时间
     * maxRetries：最多重试次数
     *
     *
     * ExponentialBackoffRetry：重试一定次数，每次重试时间依次递增
     * RetryNTimes：重试N次
     * RetryOneTime：重试一次
     * RetryUntilElapsed：重试一定时间
     */
    RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);

    @org.junit.Before
    public void init(){
        //创建 CuratorFrameworkImpl实例
        client = CuratorFrameworkFactory.newClient(SERVER, SESSION_TIMEOUT, CONNECTION_TIMEOUT, retryPolicy);

        //启动
        client.start();
    }

    /**
     *
     * @描述：第一种监听器的添加方式: 对指定的节点进行添加操作
     * 仅仅能监控指定的本节点的数据修改,删除 操作 并且只能监听一次 --->不好
     */

    @Test
    public  void TestListenterOne() throws Exception{
        client.create().orSetData().withMode(CreateMode.PERSISTENT).forPath("/test","test".getBytes());

        // 注册观察者，当节点变动时触发
        byte[] data = client.getData().usingWatcher(new Watcher() {
            @Override
            public void process(WatchedEvent event) {
                System.out.println("获取 test 节点 监听器 : " + event);
            }
        }).forPath("/test");

        client.create().orSetData().withMode(CreateMode.PERSISTENT).forPath("/test","test".getBytes());
        Thread.sleep(1000);
        client.create().orSetData().withMode(CreateMode.PERSISTENT).forPath("/test","test".getBytes());
        Thread.sleep(1000);
        System.out.println("节点数据: "+ new String(data));
        Thread.sleep(10000);
    }





    /**
     *
     * @描述：第二种监听器的添加方式: Cache 的三种实现
     *   Path Cache：监视一个路径下1）孩子结点的创建、2）删除，3）以及结点数据的更新。
     *                  产生的事件会传递给注册的PathChildrenCacheListener。
     *  Node Cache：监视一个结点的创建、更新、删除，并将结点的数据缓存在本地。
     *  Tree Cache：Path Cache和Node Cache的“合体”，监视路径下的创建、更新、删除事件，并缓存路径下所有孩子结点的数据。
     */

    //1.path Cache  连接  路径  是否获取数据
    //能监听所有的字节点 且是无限监听的模式 但是 指定目录下节点的子节点不再监听
    @Test
    public void setListenterTwoOne() throws Exception{
        PathChildrenCache childrenCache = new PathChildrenCache(client, "/test", true);
        PathChildrenCacheListener childrenCacheListener = new PathChildrenCacheListener() {
            @Override
            public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) throws Exception {
                System.out.println("开始进行事件分析:-----");
                ChildData data = event.getData();
                switch (event.getType()) {
                    case CHILD_ADDED:
                        System.out.println("CHILD_ADDED : "+ data.getPath() +"  数据:"+ data.getData());
                        break;
                    case CHILD_REMOVED:
                        System.out.println("CHILD_REMOVED : "+ data.getPath() +"  数据:"+ data.getData());
                        break;
                    case CHILD_UPDATED:
                        System.out.println("CHILD_UPDATED : "+ data.getPath() +"  数据:"+ data.getData());
                        break;
                    case INITIALIZED:
                        System.out.println("INITIALIZED : "+ data.getPath() +"  数据:"+ data.getData());
                        break;
                    default:
                        break;
                }
            }
        };
        childrenCache.getListenable().addListener(childrenCacheListener);
        System.out.println("Register zk watcher successfully!");
        childrenCache.start(PathChildrenCache.StartMode.POST_INITIALIZED_EVENT);

        //创建一个节点
        client.create().orSetData().withMode(CreateMode.PERSISTENT).forPath("/test","test".getBytes());

        client.create().orSetData().withMode(CreateMode.EPHEMERAL).forPath("/test/node01","enjoy".getBytes());
        Thread.sleep(1000);
        client.create().orSetData().withMode(CreateMode.EPHEMERAL).forPath("/test/node02","deer".getBytes());
        Thread.sleep(1000);
        client.create().orSetData().withMode(CreateMode.EPHEMERAL).forPath("/test/node02","demo".getBytes());
        Thread.sleep(1000);
        client.delete().forPath("/test/node02");
        Thread.sleep(10000);
    }

    //2.Node Cache  监控本节点的变化情况   连接 目录 是否压缩
    //监听本节点的变化  节点可以进行修改操作  删除节点后会再次创建(空节点)
    @Test
    public void setListenterTwoTwo() throws Exception{
        ExecutorService pool = Executors.newCachedThreadPool();
        //设置节点的cache
        final NodeCache nodeCache = new NodeCache(client, "/test", false);
        nodeCache.getListenable().addListener(new NodeCacheListener() {
            @Override
            public void nodeChanged() throws Exception {
                System.out.println("the test node is change and result is :");
                System.out.println("path : "+nodeCache.getCurrentData().getPath());
                System.out.println("data : "+new String(nodeCache.getCurrentData().getData()));
                System.out.println("stat : "+nodeCache.getCurrentData().getStat());
            }
        });
        nodeCache.start();

        client.create().orSetData().withMode(CreateMode.PERSISTENT).forPath("/test","test".getBytes());
        Thread.sleep(1000);
        client.create().orSetData().withMode(CreateMode.PERSISTENT).forPath("/test","enjoy".getBytes());
        Thread.sleep(10000);
    }
    //3.Tree Cache
    // 监控 指定节点和节点下的所有的节点的变化--无限监听  可以进行本节点的删除(不在创建)
    @Test
    public void TestListenterTwoThree() throws Exception{
        ExecutorService pool = Executors.newCachedThreadPool();
        //设置节点的cache
        TreeCache treeCache = new TreeCache(client, "/test");
        //设置监听器和处理过程
        treeCache.getListenable().addListener(new TreeCacheListener() {
            @Override
            public void childEvent(CuratorFramework client, TreeCacheEvent event) throws Exception {
                ChildData data = event.getData();
                if(data !=null){
                    switch (event.getType()) {
                        case NODE_ADDED:
                            System.out.println("NODE_ADDED : "+ data.getPath() +"  数据:"+ new String(data.getData()));
                            break;
                        case NODE_REMOVED:
                            System.out.println("NODE_REMOVED : "+ data.getPath() +"  数据:"+ new String(data.getData()));
                            break;
                        case NODE_UPDATED:
                            System.out.println("NODE_UPDATED : "+ data.getPath() +"  数据:"+ new String(data.getData()));
                            break;

                        default:
                            break;
                    }
                }else{
                    System.out.println( "data is null : "+ event.getType());
                }
            }
        });
        //开始监听
        treeCache.start();

        //创建一个节点
        client.create().orSetData().withMode(CreateMode.PERSISTENT).forPath("/test","test".getBytes());

        Thread.sleep(1000);
        client.create().orSetData().withMode(CreateMode.EPHEMERAL).forPath("/test/node01","enjoy".getBytes());
        Thread.sleep(1000);
        client.create().orSetData().withMode(CreateMode.EPHEMERAL).forPath("/test/node01","deer".getBytes());

        Thread.sleep(1000);
        client.create().orSetData().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL).forPath("/test/node02/node02_2","deer".getBytes());

        Thread.sleep(10000);

    }




}
