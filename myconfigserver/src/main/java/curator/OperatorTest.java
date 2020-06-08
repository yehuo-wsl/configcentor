package curator;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.api.BackgroundCallback;
import org.apache.curator.framework.api.CuratorEvent;
import org.apache.curator.framework.api.CuratorListener;
import org.apache.curator.framework.api.transaction.CuratorOp;
import org.apache.curator.framework.api.transaction.CuratorTransactionResult;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.Stat;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

/**
 *  测试Apache Curator框架的基本用法
 */
public class OperatorTest {
    //ZooKeeper服务地址
    private static final String SERVER = "127.0.0.1:2181";

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
     * 测试创建节点
     * @throws Exception
     */
    @Test
    public void testCreate() throws Exception{
        //创建永久节点
        client.create().forPath("/curator","/curator data".getBytes());

        //创建永久有序节点
        client.create().withMode(CreateMode.PERSISTENT_SEQUENTIAL).forPath("/curator_sequential","/curator_sequential data".getBytes());

        //创建临时节点
        client.create().withMode(CreateMode.EPHEMERAL)
                .forPath("/curator/ephemeral","/curator/ephemeral data".getBytes());

        //创建临时有序节点
        client.create().withMode(CreateMode.EPHEMERAL_SEQUENTIAL)
                .forPath("/curator/ephemeral_path1","/curator/ephemeral_path1 data".getBytes());

    }



    /**
     * 测试检查某个节点是否存在
     * @throws Exception
     */
    @Test
    public void testCheck() throws Exception{
        Stat stat1 = client.checkExists().forPath("/curator");
        Stat stat2 = client.checkExists().forPath("/curator2");

        System.out.println("'/curator'是否存在： " + (stat1 != null ? true : false));
        System.out.println("'/curator2'是否存在： " + (stat2 != null ? true : false));
    }


    /**
     * 测试异步设置节点数据
     * @throws Exception
     */
    @Test
    public void testSetDataAsync() throws Exception{
        //创建监听器
        CuratorListener listener = new CuratorListener() {

            @Override
            public void eventReceived(CuratorFramework client, CuratorEvent event)
                    throws Exception {
                System.out.println(event.getPath());
            }
        };

        //添加监听器
        client.getCuratorListenable().addListener(listener);

        //异步设置某个节点数据
        client.setData().inBackground().forPath("/curator","sync".getBytes());

        //为了防止单元测试结束从而看不到异步执行结果，因此暂停10秒
        Thread.sleep(10000);
    }



    /**
     * 测试另一种异步执行获取通知的方式
     * @throws Exception
     */
    @Test
    public void testSetDataAsyncWithCallback() throws Exception{
        BackgroundCallback callback = new BackgroundCallback() {

            @Override
            public void processResult(CuratorFramework client, CuratorEvent event)
                    throws Exception {
                System.out.println(event.getPath());
            }
        };

        //异步设置某个节点数据
        client.setData().inBackground(callback).forPath("/curator","/curator modified data with Callback".getBytes());

        //为了防止单元测试结束从而看不到异步执行结果，因此暂停10秒
        Thread.sleep(10000);
    }


    /**
     * 测试删除节点
     * @throws Exception
     */
    @Test
    public void testDelete() throws Exception{
        //创建测试节点
        client.create().orSetData().creatingParentsIfNeeded()
                .forPath("/curator/del_key1","/curator/del_key1 data".getBytes());

        client.create().orSetData().creatingParentsIfNeeded()
                .forPath("/curator/del_key2","/curator/del_key2 data".getBytes());

        client.create().forPath("/curator/del_key2/test_key","test_key data".getBytes());


        //删除该节点
        client.delete().forPath("/curator/del_key1");

        //级联删除子节点
        client.delete().guaranteed().deletingChildrenIfNeeded().forPath("/curator/del_key2");
    }


    /*
     * 测试事务管理：碰到异常，事务会回滚
     * @throws Exception
     */
    @Test
    public void testTransaction() throws Exception{
        //定义几个基本操作
        CuratorOp createOp = client.transactionOp().create()
                .forPath("/curator/one_path","some data".getBytes());

        CuratorOp setDataOp = client.transactionOp().setData()
                .forPath("/curator","other data".getBytes());

        CuratorOp deleteOp = client.transactionOp().delete()
                .forPath("/curator");

        //事务执行结果
        List<CuratorTransactionResult> results = client.transaction()
                .forOperations(createOp,setDataOp,deleteOp);

        //遍历输出结果
        for(CuratorTransactionResult result : results){
            System.out.println("执行结果是： " + result.getForPath() + "--" + result.getType());
        }
    }

}

