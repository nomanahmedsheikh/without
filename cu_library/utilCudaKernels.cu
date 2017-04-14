extern "C"
__global__ void initKernel(int *d_A, long size, int val)
{
  long idx = blockIdx.x * blockDim.x + threadIdx.x;

  if(idx < size)
    d_A[idx] = val;
}

extern "C"
__global__ void regularCompactKernel(int *d_A, int *d_B, long size, int intervalSize)
{
  long idx = blockIdx.x * blockDim.x + threadIdx.x;

  if(idx < size)
    d_B[idx] = d_A[idx * intervalSize];
}

extern "C"
__global__ void sumKernel(int *d_A, long size)
{
  extern __shared__ float A[];

  long idx = blockIdx.x * blockDim.x + threadIdx.x;

  if(idx < size)
    A[threadIdx.x] = d_A[idx];

  __syncthreads();

  int iters = ceil(log2((float)blockDim.x));
  int n = 1, m = 2;

  for(int i = 0; i < iters; i++)
    {
      if((threadIdx.x + n) < blockDim.x && (threadIdx.x & (m-1)) == 0)
	A[threadIdx.x] += A[threadIdx.x + n];
      n <<= 1;
      m <<= 1;
      __syncthreads();
    }

  if(threadIdx.x == 0)
    d_A[idx] = A[0];
}
